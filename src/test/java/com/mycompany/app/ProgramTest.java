package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProgramTest {

    @Test
    void mainRunsInHeadlessMode() {
        Program.main(new String[0]);
        assertTrue(true);
    }

    @Test
    void boardStartsEmpty() {
        Game g = new Game();
        for (int r = 0; r < Game.SIZE; r++) {
            for (int c = 0; c < Game.SIZE; c++) {
                assertEquals(Game.EMPTY, g.getCell(r, c));
            }
        }
    }

    @Test
    void firstPlayerIsCross() {
        assertEquals(Game.X, new Game().getCurrentPlayer());
    }

    @Test
    void gameActiveAtStart() {
        assertFalse(new Game().isGameOver());
    }

    @Test
    void legalMoveAccepted() {
        Game g = new Game();
        assertTrue(g.placeMove(1, 1));
        assertEquals(Game.X, g.getCell(1, 1));
    }

    @Test
    void offBoardMoveRejected() {
        Game g = new Game();
        assertFalse(g.placeMove(-1, 0));
        assertFalse(g.placeMove(0, 3));
    }

    @Test
    void occupiedCellRejected() {
        Game g = new Game();
        g.placeMove(0, 0);
        assertFalse(g.placeMove(0, 0));
    }

    @Test
    void turnSwapsAfterMove() {
        Game g = new Game();
        g.placeMove(0, 0);
        assertEquals(Game.O, g.getCurrentPlayer());
    }

    @Test
    void resetRestoresBoard() {
        Game g = new Game();
        g.placeMove(2, 2);
        g.reset();
        assertEquals(Game.EMPTY, g.getCell(2, 2));
        assertEquals(Game.X, g.getCurrentPlayer());
    }

    @Test
    void crossWinsRow() {
        Game g = new Game();
        fillRow(g, 0, Game.X);
        assertEquals(GameState.X_WON, g.getState());
    }

    @Test
    void ringWinsColumn() {
        Game g = new Game();
        fillCol(g, 1, Game.O);
        assertEquals(GameState.O_WON, g.getState());
    }

    @Test
    void crossWinsDiagonal() {
        Game g = new Game();
        g.setCellForTest(0, 0, Game.X);
        g.setCellForTest(1, 1, Game.X);
        g.setCellForTest(2, 2, Game.X);
        assertEquals(GameState.X_WON, g.getState());
    }

    @Test
    void drawDetected() {
        assertEquals(GameState.DRAW, filledDraw().getState());
    }

    @Test
    void boardStringFormatted() {
        Game g = new Game();
        g.placeMove(0, 0);
        assertEquals("X|-|-\n-|-|-\n-|-|-", g.boardToString());
    }

    @Test
    void minimaxRatesWinForBot() {
        Game g = new Game();
        fillRow(g, 2, Game.X);
        assertEquals(100, g.minimax(true, Game.X));
    }

    @Test
    void minimaxRatesLossForBot() {
        Game g = new Game();
        fillRow(g, 2, Game.X);
        assertEquals(-100, g.minimax(true, Game.O));
    }

    @Test
    void minimaxRatesDraw() {
        assertEquals(0, filledDraw().minimax(true, Game.X));
    }

    @Test
    void aiBlocksThreat() {
        Game g = new Game();
        g.setCellForTest(0, 0, Game.X);
        g.setCellForTest(0, 1, Game.X);
        g.setCellForTest(1, 1, Game.O);
        assertEquals(new Move(0, 2), g.findBestMove(Game.O));
    }

    @Test
    void aiCompletesLine() {
        Game g = new Game();
        g.setCellForTest(0, 0, Game.O);
        g.setCellForTest(0, 1, Game.O);
        g.setCellForTest(1, 1, Game.X);
        Move step = g.findBestMove(Game.O);
        g.setCellForTest(step.row(), step.col(), Game.O);
        assertEquals(GameState.O_WON, g.getState());
    }

    @Test
    void noMoveOnFullBoard() {
        assertNull(filledDraw().findBestMove(Game.X));
    }

    @Test
    void openMovesListed() {
        Game g = new Game();
        g.placeMove(0, 0);
        assertEquals(8, g.getAvailableMoves().size());
    }

    @Test
    void winnerResolved() {
        Game g = new Game();
        fillCol(g, 2, Game.O);
        assertEquals(Character.valueOf(Game.O), g.getWinner());
    }

    @Test
    void emptyCellCheck() {
        Game g = new Game();
        assertTrue(g.isEmptyCell(1, 2));
        g.placeMove(1, 2);
        assertFalse(g.isEmptyCell(1, 2));
    }

    @Test
    void outOfRangeThrows() {
        Game g = new Game();
        assertThrows(IllegalArgumentException.class, () -> g.getCell(4, 0));
    }

    @Test
    void badTestMarkThrows() {
        Game g = new Game();
        assertThrows(IllegalArgumentException.class, () -> g.setCellForTest(0, 0, 'Z'));
    }

    @Test
    void moveEqualityWorks() {
        Move a = new Move(1, 2);
        Move b = new Move(1, 2);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void moveToStringShowsCoords() {
        assertEquals("(2, 0)", new Move(2, 0).toString());
    }

    @Test
    void upperBoundFindsWinningScore() {
        Game g = new Game();
        fillRow(g, 1, Game.X);
        assertEquals(100, g.upperBound(Game.X));
    }

    @Test
    void lowerBoundFindsLosingScore() {
        Game g = new Game();
        fillRow(g, 1, Game.X);
        assertEquals(-100, g.lowerBound(Game.O));
    }

    @Test
    void availableMovesContainCorner() {
        Game g = new Game();
        g.switchPlayer();
        assertEquals(Game.O, g.getCurrentPlayer());
        assertTrue(g.getAvailableMoves().contains(new Move(2, 2)));
        assertFalse(g.isFullBoard());
        assertTrue(g.isValidMove(0, 0));
        g.placeMove(0, 0);
        assertFalse(g.isValidMove(0, 0));
        fillRow(g, 1, Game.X);
        assertFalse(g.isValidMove(2, 2));
    }

    private static void fillRow(Game g, int row, char mark) {
        for (int c = 0; c < Game.SIZE; c++) {
            g.setCellForTest(row, c, mark);
        }
    }

    private static void fillCol(Game g, int col, char mark) {
        for (int r = 0; r < Game.SIZE; r++) {
            g.setCellForTest(r, col, mark);
        }
    }

    private static Game filledDraw() {
        Game g = new Game();
        char[][] layout = {
            {Game.X, Game.O, Game.X},
            {Game.X, Game.O, Game.O},
            {Game.O, Game.X, Game.X}
        };
        for (int r = 0; r < Game.SIZE; r++) {
            for (int c = 0; c < Game.SIZE; c++) {
                g.setCellForTest(r, c, layout[r][c]);
            }
        }
        return g;
    }
}
