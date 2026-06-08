package com.mycompany.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

enum GameState {
    PLAYING,
    X_WON,
    O_WON,
    DRAW
}

final class Move {
    private final int slotRow;
    private final int slotCol;

    Move(int row, int col) {
        slotRow = row;
        slotCol = col;
    }

    int row() {
        return slotRow;
    }

    int col() {
        return slotCol;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Move)) {
            return false;
        }
        Move other = (Move) obj;
        return slotRow == other.slotRow && slotCol == other.slotCol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotRow, slotCol);
    }

    @Override
    public String toString() {
        return "(" + slotRow + ", " + slotCol + ")";
    }
}

class Game {
    static final char X = 'X';
    static final char O = 'O';
    static final char EMPTY = ' ';
    static final int SIZE = 3;
    private static final int WIN_WEIGHT = 100;

    private final char[][] cells;
    private char activeMark;
    char recentMark;
    int exploredNodes;

    Game() {
        cells = new char[SIZE][SIZE];
        clearAll();
    }

    void clearAll() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c] = EMPTY;
            }
        }
        activeMark = X;
        recentMark = EMPTY;
    }

    void reset() {
        clearAll();
    }

    char getCurrentPlayer() {
        return activeMark;
    }

    char getCell(int row, int col) {
        guardBounds(row, col);
        return cells[row][col];
    }

    boolean isEmptyCell(int row, int col) {
        return inside(row, col) && cells[row][col] == EMPTY;
    }

    boolean isValidMove(int row, int col) {
        return inside(row, col) && cells[row][col] == EMPTY && !isGameOver();
    }

    boolean placeMove(int row, int col) {
        if (!isValidMove(row, col)) {
            return false;
        }
        cells[row][col] = activeMark;
        recentMark = activeMark;
        if (!isGameOver()) {
            flipMark();
        }
        return true;
    }

    void setCellForTest(int row, int col, char mark) {
        guardBounds(row, col);
        if (mark != X && mark != O && mark != EMPTY) {
            throw new IllegalArgumentException("bad mark");
        }
        cells[row][col] = mark;
        if (mark != EMPTY) {
            recentMark = mark;
        }
    }

    void setCurrentPlayerForTest(char mark) {
        if (mark != X && mark != O) {
            throw new IllegalArgumentException("bad mark");
        }
        activeMark = mark;
    }

    void switchPlayer() {
        flipMark();
    }

    Character getWinner() {
        if (ownsLine(X)) {
            return X;
        }
        if (ownsLine(O)) {
            return O;
        }
        return null;
    }

    GameState getState() {
        Character leader = getWinner();
        if (leader != null) {
            return leader == X ? GameState.X_WON : GameState.O_WON;
        }
        if (noBlanksLeft()) {
            return GameState.DRAW;
        }
        return GameState.PLAYING;
    }

    boolean isGameOver() {
        return getState() != GameState.PLAYING;
    }

    boolean isFullBoard() {
        return noBlanksLeft();
    }

    List<Move> getAvailableMoves() {
        List<Move> pool = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (cells[r][c] == EMPTY) {
                    pool.add(new Move(r, c));
                }
            }
        }
        return pool;
    }

    Move findBestMove(char bot) {
        List<Move> pool = getAvailableMoves();
        if (pool.isEmpty()) {
            return null;
        }
        int bestRank = -WIN_WEIGHT;
        int tieIndex = 0;
        Move[] tied = new Move[9];
        exploredNodes = 0;
        for (Move step : pool) {
            apply(step, bot);
            int rank = minimax(false, bot);
            undo(step);
            if (rank > bestRank) {
                bestRank = rank;
                tieIndex = 0;
                tied[tieIndex] = step;
            } else if (rank == bestRank) {
                tied[++tieIndex] = step;
            }
            System.out.printf("\nminimax: %3d(%1d) ", flatIndex(step) + 1, rank);
        }
        Move chosen = tied[0];
        System.out.printf("\nminimax best: %3d(%1d) ", flatIndex(chosen) + 1, bestRank);
        System.out.printf("Steps counted: %d", exploredNodes);
        exploredNodes = 0;
        return chosen;
    }

    int minimax(boolean maximizing, char bot) {
        GameState status = getState();
        if (status != GameState.PLAYING) {
            return terminalValue(status, bot);
        }
        exploredNodes++;
        char actor = maximizing ? bot : rival(bot);
        int bound = maximizing ? -WIN_WEIGHT : WIN_WEIGHT;
        for (Move step : getAvailableMoves()) {
            apply(step, actor);
            int child = minimax(!maximizing, bot);
            undo(step);
            if (maximizing) {
                bound = Math.max(bound, child);
            } else {
                bound = Math.min(bound, child);
            }
        }
        return bound;
    }

    int lowerBound(char bot) {
        GameState status = getState();
        if (status != GameState.PLAYING) {
            return terminalValue(status, bot);
        }
        exploredNodes++;
        int bound = WIN_WEIGHT;
        for (Move step : getAvailableMoves()) {
            apply(step, rival(bot));
            bound = Math.min(bound, upperBound(bot));
            undo(step);
        }
        return bound;
    }

    int upperBound(char bot) {
        GameState status = getState();
        if (status != GameState.PLAYING) {
            return terminalValue(status, bot);
        }
        exploredNodes++;
        int bound = -WIN_WEIGHT;
        for (Move step : getAvailableMoves()) {
            apply(step, bot);
            bound = Math.max(bound, lowerBound(bot));
            undo(step);
        }
        return bound;
    }

    String boardToString() {
        StringBuilder buf = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            if (r > 0) {
                buf.append('\n');
            }
            for (int c = 0; c < SIZE; c++) {
                if (c > 0) {
                    buf.append('|');
                }
                char v = cells[r][c];
                buf.append(v == EMPTY ? '-' : v);
            }
        }
        return buf.toString();
    }

    private int terminalValue(GameState status, char bot) {
        boolean won = (status == GameState.X_WON && bot == X)
                || (status == GameState.O_WON && bot == O);
        boolean lost = (status == GameState.X_WON && bot == O)
                || (status == GameState.O_WON && bot == X);
        if (won) {
            return WIN_WEIGHT;
        }
        if (lost) {
            return -WIN_WEIGHT;
        }
        return 0;
    }

    private boolean ownsLine(char mark) {
        for (int i = 0; i < SIZE; i++) {
            if (cells[i][0] == mark && cells[i][1] == mark && cells[i][2] == mark) {
                return true;
            }
            if (cells[0][i] == mark && cells[1][i] == mark && cells[2][i] == mark) {
                return true;
            }
        }
        return (cells[0][0] == mark && cells[1][1] == mark && cells[2][2] == mark)
                || (cells[0][2] == mark && cells[1][1] == mark && cells[2][0] == mark);
    }

    private boolean inside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    private void guardBounds(int row, int col) {
        if (!inside(row, col)) {
            throw new IllegalArgumentException("out of range");
        }
    }

    private boolean noBlanksLeft() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (cells[r][c] == EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    private char rival(char mark) {
        return mark == X ? O : X;
    }

    private void apply(Move step, char mark) {
        cells[step.row()][step.col()] = mark;
        recentMark = mark;
    }

    private void undo(Move step) {
        cells[step.row()][step.col()] = EMPTY;
    }

    private void flipMark() {
        activeMark = activeMark == X ? O : X;
    }

    private int flatIndex(Move step) {
        return step.row() * SIZE + step.col();
    }
}

public class Program implements ActionListener {

    private static Game match;
    private static JButton[] pads;
    private static boolean humanTurn;

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            Game demo = new Game();
            demo.placeMove(0, 0);
            Move reply = demo.findBestMove(Game.O);
            if (reply != null) {
                demo.placeMove(reply.row(), reply.col());
            }
            System.out.println(demo.boardToString());
            System.out.println("State: " + demo.getState());
            return;
        }
        match = new Game();
        humanTurn = true;
        pads = new JButton[9];
        JFrame frame = new JFrame("Крестики-нолики");
        JPanel board = new JPanel(new GridLayout(3, 3, 6, 6));
        board.setBackground(new Color(22, 27, 34));
        board.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        for (int i = 0; i < 9; i++) {
            pads[i] = new JButton(" ");
            pads[i].setFont(new Font("Verdana", Font.BOLD, 42));
            pads[i].setBackground(new Color(45, 52, 64));
            pads[i].setForeground(new Color(230, 237, 243));
            pads[i].setFocusPainted(false);
            pads[i].addActionListener(new Program());
            board.add(pads[i]);
        }
        frame.add(board);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(90, 70, 500, 540);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent evt) {
        if (!humanTurn) {
            return;
        }
        JButton source = (JButton) evt.getSource();
        int idx = -1;
        for (int i = 0; i < 9; i++) {
            if (pads[i] == source) {
                idx = i;
                break;
            }
        }
        if (idx < 0 || !pads[idx].isEnabled()) {
            return;
        }
        int row = idx / Game.SIZE;
        int col = idx % Game.SIZE;
        if (!match.placeMove(row, col)) {
            return;
        }
        paintPad(idx, Game.X);
        finishIfNeeded();
        if (match.isGameOver()) {
            return;
        }
        humanTurn = false;
        Move aiStep = match.findBestMove(Game.O);
        if (aiStep != null) {
            match.placeMove(aiStep.row(), aiStep.col());
            paintPad(aiStep.row() * Game.SIZE + aiStep.col(), Game.O);
        }
        humanTurn = true;
        finishIfNeeded();
    }

    private static void paintPad(int idx, char mark) {
        pads[idx].setText(String.valueOf(mark));
        pads[idx].setEnabled(false);
        pads[idx].setForeground(mark == Game.X
                ? new Color(248, 81, 73)
                : new Color(88, 166, 255));
    }

    private static void finishIfNeeded() {
        GameState status = match.getState();
        if (status == GameState.X_WON) {
            JOptionPane.showMessageDialog(null, "Победа крестиков", "Итог", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        if (status == GameState.O_WON) {
            JOptionPane.showMessageDialog(null, "Победа ноликов", "Итог", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        if (status == GameState.DRAW) {
            JOptionPane.showMessageDialog(null, "Ничья", "Итог", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
}
