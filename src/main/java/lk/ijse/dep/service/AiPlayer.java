package lk.ijse.dep.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AiPlayer extends Player {

    private static final double C = 1.41; // Optimized exploration constant
    private static final long THINKING_TIME_MS = 5000;   // 5 seconds for much deeper search
    private static final Random random = new Random();
    private static final int[] COLUMN_PRIORITY = {3, 2, 4, 1, 5, 0, 6}; // Center-first strategy

    public AiPlayer(Board board) {
        super(board);
    }

    @Override
    public void movePiece(int col) {
        int bestCol = Mcts.findBestMove((BoardImpl) board);

        if (board.isLegalMove(bestCol)) {
            board.updateMove(bestCol, Piece.GREEN);
            board.getBoardUI().update(bestCol, false);

            Winner winner = board.findWinner();
            if (winner.getWinningPiece() != Piece.EMPTY) {
                board.getBoardUI().notifyWinner(winner);
            } else if (!board.existLegalMoves()) {
                board.getBoardUI().notifyWinner(new Winner(Piece.EMPTY, -1, -1, -1, -1));
            }
        }
    }

    // =========================== MCTS ===========================
    private static class Mcts {

        public static int findBestMove(BoardImpl currentBoard) {
            // Check for immediate winning move
            int winningMove = findImmediateWinningMove(currentBoard, Piece.GREEN);
            if (winningMove != -1) {
                System.out.println("AI found immediate winning move: Column " + winningMove);
                return winningMove;
            }

            // Check for blocking opponent's winning move
            int blockingMove = findImmediateWinningMove(currentBoard, Piece.BLUE);
            if (blockingMove != -1) {
                System.out.println("AI blocking opponent's winning move: Column " + blockingMove);
                return blockingMove;
            }

            // Proceed with MCTS
            BoardImpl rootBoardCopy = copyBoard(currentBoard);
            Node root = new Node(rootBoardCopy, -1, Piece.BLUE);
            root.parent = null;

            long deadline = System.currentTimeMillis() + THINKING_TIME_MS;
            int iterations = 0;

            while (System.currentTimeMillis() < deadline) {
                Node selected = selectPromisingNode(root);

                if (!selected.isTerminal()) {
                    expandNode(selected);
                }

                Node nodeToSimulate = selected;
                if (!selected.children.isEmpty()) {
                    // Prefer more promising children for simulation
                    nodeToSimulate = selectSimulationNode(selected);
                }

                Piece result = simulateIntelligentPlayout(nodeToSimulate);
                backpropagate(nodeToSimulate, result);
                iterations++;
            }

            // Advanced move selection
            Node bestChild = selectBestChild(root);

            System.out.println("MCTS completed " + iterations + " iterations");
            if (bestChild != null) {
                double winRate = bestChild.visits > 0 ? (bestChild.wins / bestChild.visits) * 100 : 0;
                System.out.println("Best move: Column " + bestChild.column +
                        " (Visits: " + bestChild.visits +
                        ", Win rate: " + String.format("%.1f%%", winRate) + ")");
            }

            return bestChild != null ? bestChild.column : getStrategicMove(currentBoard);
        }

        private static Node selectBestChild(Node root) {
            if (root.children.isEmpty()) return null;

            // Use robust child selection (highest visit count)
            return root.children.stream()
                    .max((a, b) -> {
                        // Primary: visit count (robustness)
                        int visitCompare = Integer.compare(a.visits, b.visits);
                        if (visitCompare != 0) return visitCompare;

                        // Tiebreaker: win rate
                        double aWinRate = a.visits > 0 ? a.wins / a.visits : 0;
                        double bWinRate = b.visits > 0 ? b.wins / b.visits : 0;
                        int winRateCompare = Double.compare(aWinRate, bWinRate);
                        if (winRateCompare != 0) return winRateCompare;

                        // Final tiebreaker: prefer center columns
                        return Integer.compare(
                                Math.abs(b.column - Board.NUM_OF_COLS / 2),
                                Math.abs(a.column - Board.NUM_OF_COLS / 2)
                        );
                    })
                    .orElse(null);
        }

        private static Node selectSimulationNode(Node parent) {
            // Select child with best UCB1 score for simulation
            return parent.children.stream()
                    .max((a, b) -> {
                        double aScore = a.visits > 0 ? a.wins / a.visits : 0.5;
                        double bScore = b.visits > 0 ? b.wins / b.visits : 0.5;
                        return Double.compare(aScore, bScore);
                    })
                    .orElse(parent.children.get(random.nextInt(parent.children.size())));
        }

        private static Node selectPromisingNode(Node node) {
            while (!node.children.isEmpty() && !node.isTerminal()) {
                node = node.children.stream()
                        .max((a, b) -> Double.compare(uct(a), uct(b)))
                        .orElse(node);
            }
            return node;
        }

        private static void expandNode(Node node) {
            // Expand in strategic order (center first)
            for (int priority : COLUMN_PRIORITY) {
                if (priority < Board.NUM_OF_COLS && node.board.isLegalMove(priority)) {
                    BoardImpl newBoard = copyBoard(node.board);
                    newBoard.updateMove(priority, node.nextToMove);

                    Piece nextPlayer = node.nextToMove == Piece.GREEN ? Piece.BLUE : Piece.GREEN;
                    Node child = new Node(newBoard, priority, nextPlayer);
                    child.parent = node;
                    node.children.add(child);
                }
            }
        }

        private static Piece simulateIntelligentPlayout(Node node) {
            BoardImpl board = copyBoard(node.board);
            Piece current = node.nextToMove;

            Winner immediateWinner = board.findWinner();
            if (immediateWinner.getWinningPiece() != Piece.EMPTY) {
                return immediateWinner.getWinningPiece();
            }

            int movesPlayed = 0;
            int maxMoves = 42;

            while (board.existLegalMoves() && movesPlayed < maxMoves) {
                Winner winner = board.findWinner();
                if (winner.getWinningPiece() != Piece.EMPTY) {
                    return winner.getWinningPiece();
                }

                // Intelligent move selection during playout
                int move = selectIntelligentMove(board, current);

                board.updateMove(move, current);
                current = (current == Piece.GREEN) ? Piece.BLUE : Piece.GREEN;
                movesPlayed++;
            }

            Winner finalWinner = board.findWinner();
            return finalWinner.getWinningPiece();
        }

        private static int selectIntelligentMove(BoardImpl board, Piece current) {
            Piece opponent = (current == Piece.GREEN) ? Piece.BLUE : Piece.GREEN;

            // 1. Check for winning move
            int winMove = findImmediateWinningMove(board, current);
            if (winMove != -1) return winMove;

            // 2. Block opponent's winning move
            int blockMove = findImmediateWinningMove(board, opponent);
            if (blockMove != -1) return blockMove;

            // 3. Check for threats (moves that create multiple winning opportunities)
            int threatMove = findThreatMove(board, current);
            if (threatMove != -1) return threatMove;

            // 4. Block opponent's threats
            int blockThreat = findThreatMove(board, opponent);
            if (blockThreat != -1) return blockThreat;

            // 5. Prefer center columns with some randomness (80% strategic, 20% random)
            if (random.nextDouble() < 0.8) {
                for (int col : COLUMN_PRIORITY) {
                    if (col < Board.NUM_OF_COLS && board.isLegalMove(col)) {
                        return col;
                    }
                }
            }

            // 6. Random legal move
            List<Integer> moves = new ArrayList<>();
            for (int c = 0; c < Board.NUM_OF_COLS; c++) {
                if (board.isLegalMove(c)) moves.add(c);
            }
            return moves.isEmpty() ? 0 : moves.get(random.nextInt(moves.size()));
        }

        private static int findImmediateWinningMove(BoardImpl board, Piece piece) {
            for (int col = 0; col < Board.NUM_OF_COLS; col++) {
                if (board.isLegalMove(col)) {
                    BoardImpl testBoard = copyBoard(board);
                    testBoard.updateMove(col, piece);
                    if (testBoard.findWinner().getWinningPiece() == piece) {
                        return col;
                    }
                }
            }
            return -1;
        }

        private static int findThreatMove(BoardImpl board, Piece piece) {
            // Find moves that create multiple winning opportunities
            for (int col = 0; col < Board.NUM_OF_COLS; col++) {
                if (board.isLegalMove(col)) {
                    BoardImpl testBoard = copyBoard(board);
                    testBoard.updateMove(col, piece);

                    int winningMoves = 0;
                    for (int nextCol = 0; nextCol < Board.NUM_OF_COLS; nextCol++) {
                        if (testBoard.isLegalMove(nextCol)) {
                            BoardImpl testBoard2 = copyBoard(testBoard);
                            testBoard2.updateMove(nextCol, piece);
                            if (testBoard2.findWinner().getWinningPiece() == piece) {
                                winningMoves++;
                            }
                        }
                    }

                    // If this move creates 2+ winning opportunities (fork), it's a threat
                    if (winningMoves >= 2) return col;
                }
            }
            return -1;
        }

        private static void backpropagate(Node node, Piece winner) {
            while (node != null) {
                node.visits++;

                if (winner == Piece.GREEN) {
                    node.wins += 1.0;
                } else if (winner == Piece.EMPTY) {
                    node.wins += 0.5;
                }

                node = node.parent;
            }
        }

        private static double uct(Node node) {
            if (node.visits == 0) return Double.MAX_VALUE;

            double winRate = node.wins / node.visits;
            double exploration = 0;

            if (node.parent != null && node.parent.visits > 0) {
                exploration = C * Math.sqrt(Math.log(node.parent.visits) / node.visits);
            }

            return winRate + exploration;
        }

        private static BoardImpl copyBoard(BoardImpl original) {
            BoardImpl copy = new BoardImpl(null);
            Piece[][] originalPieces = original.getPieces();
            Piece[][] copiedPieces = new Piece[Board.NUM_OF_COLS][Board.NUM_OF_ROWS];

            for (int col = 0; col < Board.NUM_OF_COLS; col++) {
                for (int row = 0; row < Board.NUM_OF_ROWS; row++) {
                    copiedPieces[col][row] = originalPieces[col][row];
                }
            }

            copy.setPieces(copiedPieces);
            return copy;
        }

        private static int getStrategicMove(BoardImpl board) {
            // Strategic fallback: prefer center columns
            for (int col : COLUMN_PRIORITY) {
                if (col < Board.NUM_OF_COLS && board.isLegalMove(col)) {
                    return col;
                }
            }

            List<Integer> legalMoves = new ArrayList<>();
            for (int col = 0; col < Board.NUM_OF_COLS; col++) {
                if (board.isLegalMove(col)) legalMoves.add(col);
            }
            return legalMoves.isEmpty() ? 0 : legalMoves.get(random.nextInt(legalMoves.size()));
        }
    }

    // =========================== Node ===========================
    private static class Node {
        BoardImpl board;
        final int column;
        final Piece nextToMove;
        Node parent;
        final List<Node> children = new ArrayList<>();
        int visits = 0;
        double wins = 0.0;

        Node(BoardImpl board, int column, Piece nextToMove) {
            this.board = board;
            this.column = column;
            this.nextToMove = nextToMove;
        }

        boolean isTerminal() {
            return !board.existLegalMoves() || board.findWinner().getWinningPiece() != Piece.EMPTY;
        }
    }
}