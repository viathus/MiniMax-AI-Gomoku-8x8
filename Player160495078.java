import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static java.lang.Long.*;
import java.util.Arrays;
import java.util.Comparator;
/**
 * DONE:
 * Minimax AlphaBeta pruning
 * Iterative deepening
 * Heuristic evaluation
 * Bitboard representation
 * Time limit
 * Move ordering
 * 
 * TODO: 
 * Transpostion table
 * Traps
 * 
 */
class Player160495078 extends GomokuPlayer
{
    final static int SCORE_WIN = 5000;
    final static int SCORE_LOSE = -5000;
    final static int SCORE_DRAW = 0; 

    final static int OPEN_FOUR_BOTH_SIDES = 400;
    final static int OPEN_FOUR_ONE_SIDE = 25;

    final static int OPEN_THREE_BOTH_SIDES = 25;
    final static int OPEN_THREE = 70;

    final static int DEAD_SIDE = -2;
    final static int STILL_PLAYING = Integer.MAX_VALUE; 
    private static final long[] ROW_MASKS = {
            0xFF00000000000000L,
            0x00FF000000000000L,
            0x0000FF0000000000L,
            0x000000FF00000000L,
            0x00000000FF000000L,
            0x0000000000FF0000L,
            0x000000000000FF00L,
            0x00000000000000FFL,

        };
    private static final long[] COL_MASKS = {
            0x8080808080808080L,
            0x4040404040404040L,
            0x2020202020202020L,
            0x1010101010101010L,
            0x0808080808080808L,
            0x0404040404040404L,
            0x0202020202020202L,
            0x0101010101010101L,        
        };
    private static final long[] DIA_MASKS = {
            0x0810204080000000L,
            0x0408102040800000L,
            0x0204081020408000L,
            0x0102040810204080L,
            0x0001020408102040L,
            0x0000010204081020L,
            0x0000000102040810L    
        };
    private static final long[] OPPOSITE_DIA_MASKS = {
            0x0000008040201008L,
            0x0000804020100804L,
            0x0080402010080402L,
            0x8040201008040201L,
            0x4020100804020100L,
            0x2010080402010000L,
            0x1008040201000000L,                 
        };

    long timer=0;
    private boolean flag = true;

    public Move chooseMove(Color[][] board, Color me) {
        long[] bit_board = convertToLong(board,me); //Convert board to binary long 64 bits
        long current_board = bit_board[0]; //get overall board(player+opponents)
        long player = bit_board[1]; //get player board only
        Move newMove = null; //newMove we wish to try out
        int depth = 3; //Depth set at 4 
        long opponent = current_board ^ player; // player XOR with current board gives opponent board only
        refresh(); //Refresh timer so we don't go more than 10 seconds


        //No move ordering so we are picking the first 2 moves if possible.
        if(flag){
            if(Long.bitCount(current_board) == 0){
                return new Move(4,4);
            }else if(Long.bitCount(current_board) == 1){
                if(board[4][3] == null){
                    return new Move(4,3);
                }else{
                    return new Move(4,4);
                }
            }
            flag = false;
        }
        BestMove move = null;
        //iterative deepening depth is set at 4, keep increasing depth until time runs out also best possible move at each depth
        do{
            try{
                move = MiniMax(current_board,player, depth,Integer.MIN_VALUE,Integer.MAX_VALUE, true); //MiniMax with alpha beta pruning
                long temp_board = move.move  ^ current_board; //gets specific move that just happened
                newMove = new Move((Long.numberOfLeadingZeros(temp_board) / 8),(Long.numberOfLeadingZeros(temp_board) % 8)); //calculates specific move in order to place it
            }catch(NoTimeException e){
                refresh(); //Refresh timer 
                break; //break loop and place moves
            }
            depth += 1; //iterative deepening increasing depth if we still have time
        }while(checkTime() && depth < 400);
        System.err.print(newMove + ": " + move.score + " || ");
        return newMove;

    }

    //Returning a class with score and possible best move. 
    BestMove MiniMax(long board,long player, int depth, int alpha, int beta, boolean MaxPlayer) throws NoTimeException {
        Timer(); //Check if we have time
        int terminal = evaluation(board,player,!MaxPlayer); //Check if player has won or not
        if(terminal != STILL_PLAYING){
            return new BestMove(terminal); //Return score that move.
        }
        else if(depth == 0){
            return new BestMove(play_evaluation(player,~player & board,MaxPlayer)); //Calculate moves score in that position
        }else if(MaxPlayer){
            BestMove BestScore = new BestMove(Integer.MIN_VALUE);          
            for(long[] PossibleMove : PossibleMoves(board,player,MaxPlayer)){
                BestMove score = MiniMax(PossibleMove[0],PossibleMove[1], depth-1, alpha, beta, false); //Recursive call
                if(score.score > BestScore.score){
                    BestScore.score = score.score; //save score of best possible move
                    BestScore.move = PossibleMove[0]; //save best possible move
                }
                if (BestScore.score > alpha){
                    alpha = BestScore.score; //pruning
                }
                if(beta<=alpha){
                    break; //pruning
                }
            }
            return  BestScore;
        }else{
            BestMove BestScore = new BestMove(Integer.MAX_VALUE);
            for(long[] PossibleMove : PossibleMoves(board,player, MaxPlayer)){
                BestMove score = MiniMax(PossibleMove[0], PossibleMove[1],depth-1,alpha,beta, true);
                if(score.score <  BestScore.score){
                    BestScore.score = score.score;
                    BestScore.move = PossibleMove[0];
                }
                if (BestScore.score < beta){
                    beta = BestScore.score;
                }
                if(beta<=alpha){
                    break;
                }
            }
            return BestScore;
        }  
    }

    //Check if player or opponent has won by comparing it to each individual mask
    int evaluation(long board,long player, boolean MaxPlayer){ 
        long masked=0L;
        player = MaxPlayer ? player : player ^ board;
        //Compare player board with ROW masks to see if there is a win then return score accordingly 
        for(int i=0; i<ROW_MASKS.length; i++){
            masked = player & ROW_MASKS[i]; 
            if((masked & (masked >>> 1 ) & (masked >>> 2) & (masked >>> 3) & (masked >>> 4)) != 0){
                return MaxPlayer ? SCORE_WIN : SCORE_LOSE;
            }
        }
        //Compare player board with COL masks to see if there is a win then return score accordingly 
        for(int i=0; i<COL_MASKS.length; i++){
            masked = player & COL_MASKS[i]; 
            if((masked & (masked >>> 8 ) & (masked >>> 16) & (masked >>> 24) & (masked >>> 32)) != 0){
                return MaxPlayer ? SCORE_WIN : SCORE_LOSE;
            }
        }
        //Compare player board with diag masks to see if there is a win then return score accordingly 
        for(int i=0; i<DIA_MASKS.length; i++){
            masked = player & DIA_MASKS[i]; 
            if((masked & (masked >>> 7 ) & (masked >>> 14 ) & (masked >>> 21 ) & (masked >>> 28 )) != 0){
                return MaxPlayer ? SCORE_WIN : SCORE_LOSE;
            }
        }
        //Compare player board with antidiag masks to see if there is a win then return score accordingly 
        for(int i=0; i<OPPOSITE_DIA_MASKS.length; i++){
            masked = player & OPPOSITE_DIA_MASKS[i]; 
            if((masked & (masked >>> 9 ) & (masked >>> 18 ) & (masked >>> 27 ) & (masked >>> 36 )) != 0){
                return MaxPlayer ? SCORE_WIN : SCORE_LOSE;
            }
        }
        //if no win check if its a draw if no draw then we are still playing
        return board == -1 ? SCORE_DRAW : STILL_PLAYING;
    }

    //Scores each position on board to make optimal move by finding set combinations.
    int play_evaluation(long player, long opponent, boolean MaxPlayer){ 

        int score=0;
        long p_row_check = player;
        long o_row_check = opponent;

        //flip board so col becomes row
        long p_vertical_check = colToRow(player);
        long o_vertical_check = colToRow(opponent);

        //flip board so diag becomes row
        long p_diagonal_check = anticlockwise(player);
        long o_diagonal_check = anticlockwise(opponent); 

        //flip board so anti-diag becomes row
        long p_anti_diagonal_check = clockwise(player);
        long o_anti_diagonal_check = clockwise(opponent);

        //ANTI DIAGONAL CHECK
        score += formFours(p_anti_diagonal_check,o_anti_diagonal_check,MaxPlayer);
        score -= formFours(o_anti_diagonal_check,p_anti_diagonal_check,!MaxPlayer);

        //ROW CHECK
        score += formFours(p_row_check,o_row_check,MaxPlayer);
        score -= formFours(o_row_check,p_row_check,!MaxPlayer);

        //COL CHECK
        score += formFours(p_vertical_check,o_vertical_check,MaxPlayer);
        score -= formFours(o_vertical_check,p_vertical_check,!MaxPlayer);

        //DIAGONAL CHECK
        score += formFours(p_diagonal_check,o_diagonal_check,MaxPlayer);
        score -= formFours(o_diagonal_check,p_diagonal_check,!MaxPlayer);

        score += formThrees(p_anti_diagonal_check,o_anti_diagonal_check,MaxPlayer);
        score -= formThrees(o_anti_diagonal_check,p_anti_diagonal_check,!MaxPlayer);

        score += formThrees(p_row_check,o_row_check,MaxPlayer);
        score -= formThrees(o_row_check,p_row_check,!MaxPlayer);

        score += formThrees(p_diagonal_check,o_diagonal_check,MaxPlayer);
        score -= formThrees(o_diagonal_check,p_diagonal_check,!MaxPlayer);

        score += formThrees(p_vertical_check,o_vertical_check,MaxPlayer);
        score -= formThrees(o_vertical_check,p_vertical_check,!MaxPlayer);

        return score;
    }

    //Check if it can form a four with certain conditions.
    int formFours(long player, long opponent, boolean MaxPlayer){
        int score = 0;
        long checkfours = MaxPlayer ? player : opponent;
        for(int i=0; i<ROW_MASKS.length; i++){
            long pmasked = player & ROW_MASKS[i];
            checkfours = checkfours & ROW_MASKS[i];
            long omasked = opponent & ROW_MASKS[i];
            boolean checkrightflag = false;
             boolean checkleftflag = false;
           
            if((pmasked & (pmasked >>> 1 ) & (pmasked >>> 2) & (pmasked >>> 3)) != 0){  
                long checkright = ((pmasked >>> 1));
                long checkleft = ((pmasked << 1));
                 checkrightflag = ((pmasked >>> 1) & omasked) == 0;
                checkleftflag = ((pmasked << 1) & omasked) == 0;
                //IF statements to prevent AI from thinking placing on the sides counts as open three or open four
                if(i==0){
                    if(bitCount(pmasked << 1 & ROW_MASKS[i]) != bitCount(pmasked)){
                        checkleftflag = false;
                    }
                }else if(i==7){
                    if(bitCount(pmasked >>> 1 & ROW_MASKS[i]) != bitCount(pmasked)){
                        checkrightflag = false;
                    }              
                }else{
                    if((checkleft & ROW_MASKS[i-1]) != 0){
                        if(bitCount(checkleft & ROW_MASKS[i]) == 4){
                            checkleftflag = true;
                        }else{
                            checkleftflag = false;
                        }
                    }
                    if((checkright  & ROW_MASKS[i+1]) != 0){  
                        if(bitCount(checkright & ROW_MASKS[i]) == 4){
                            checkrightflag = true;
                        }else{
                            checkrightflag = false;
                        }
                    }
                }

                if(checkrightflag && checkleftflag){         
                    score += OPEN_FOUR_BOTH_SIDES;
                }else if(checkrightflag || checkleftflag){
                    score += OPEN_FOUR_ONE_SIDE;
                }else{
                    score += DEAD_SIDE;
                }  
            }
        }

        return score;
    }

    //Checks if it can form a three with certain conditions.
    int formThrees(long player, long opponent, boolean MaxPlayer){
        int score = 0;
        for(int i=0; i<ROW_MASKS.length; i++){
            long pmasked = player & ROW_MASKS[i];
            long omasked = opponent & ROW_MASKS[i];
            int original = bitCount(pmasked);
           
            if((pmasked & (pmasked >>> 1 ) & (pmasked >>> 2)) != 0){  
                long checkright = ((pmasked >>> 1));
                long checkleft = ((pmasked << 1));
                long checkright2 = ((pmasked >>> 2));
                long checkleft2 = ((pmasked << 2));

                boolean checkrightflag = ((pmasked >>> 1) & omasked) == 0;
                boolean checkleftflag = ((pmasked << 1) & omasked) == 0;
                boolean checkrightflag2 = ((pmasked >>> 2) & omasked) == 0;
                boolean checkleftflag2 = ((pmasked << 2) & omasked) == 0;
                //IF statements to prevent AI from thinking placing on the sides counts as open three or open four
                if(i==0){
                    if(bitCount((pmasked << 1) & ROW_MASKS[i]) != bitCount(pmasked) || bitCount((pmasked << 2) & ROW_MASKS[i]) != bitCount(pmasked)){
                        checkleftflag = false;
                        checkleftflag2 = false;
                    }
                }else if(i==7){
                    if(bitCount((pmasked >> 1) & ROW_MASKS[i]) != bitCount(pmasked) || bitCount((pmasked >> 2) & ROW_MASKS[i]) != bitCount(pmasked)){
                        checkrightflag = false;
                        checkrightflag2 = false;
                    }              
                }else{
                    if((checkleft & ROW_MASKS[i-1]) != 0 || (checkleft2 & ROW_MASKS[i-1]) != 0){
                        checkleftflag = false;
                        checkleftflag2 = false;
                    }
                    if((checkright  & ROW_MASKS[i+1]) != 0 || (checkright2  & ROW_MASKS[i+1]) != 0){  
                        checkrightflag = false;
                        checkrightflag2 = false;
                    }
                }
                if(checkrightflag && checkleftflag2 && checkleftflag && checkrightflag2){   
                    score += OPEN_THREE * bitCount(pmasked) ;
                }else{
                    score += DEAD_SIDE;
                }
            }
        }
        return score;
    }

    //All possiblesMoves the AI can execute and orders them highest to low
    long[][] PossibleMoves(long board, long player, boolean MaxPlayer){  
        //Set moves size equal to the amount empty spaces in board
        long[][] moves = new long[Long.bitCount(~board)][3]; 
        //Set new board with 1's as possible locations
        long possible_moves_board = ~board;

        for(int i=0; i<moves.length; i++){
            long move = Long.highestOneBit(possible_moves_board); //Get the first/highest bit as possible move
            moves[i][0] = board | move; //add the possible move to overall board
            moves[i][1] = MaxPlayer ? player | move : player;
            moves[i][2] = (int) play_evaluation(moves[i][1],~moves[i][1] &  moves[i][0], MaxPlayer);
            possible_moves_board ^= move; //remove the first/highest bit as possible move
        }
        Arrays.sort(moves, new Comparator<long[]>() {
                public int compare(long[] a, long[] b) {
                    return Double.compare(b[2], a[2]);
                }
            });
        return moves;
    }

    //Converts board to long 64 8x8
    long[] convertToLong(Color[][] board, Color me) {
        long[] new_board = new long[]{0, 0}; 
        for (int row = 0; row < GomokuBoard.ROWS; row++) {
            for (int col = 0; col < GomokuBoard.COLS; col++) {
                if (board[row][col] == null) {
                    continue;
                }
                else if (board[row][col].equals(me)) {
                    new_board[0] += 0x8000000000000000L >>> ((row * 8) + col);
                    new_board[1] += 0x8000000000000000L >>> ((row * 8) + col);
                }
                else { 
                    new_board[0] += 0x8000000000000000L >>> ((row * 8) + col);
                }
            }
        }
        return new_board;
    }

    /**
     * HELPER FUNCTION to convert bit clockwise:
     * 1 0 0            1 1 1  
     * 0 1 0    --->    0 0 0
     * 0 0 1            0 0 0
     * FROM https://www.chessprogramming.org/Flipping_Mirroring_and_Rotating                                                 
     */
    long clockwise(long x) {

        long k1 = 0xAAAAAAAAAAAAAAAAL;
        long k2 = 0xCCCCCCCCCCCCCCCCL;
        long k4 = 0xF0F0F0F0F0F0F0F0L;
        x ^= k1 & (x ^ rotateRight(x, 8));
        x ^= k2 & (x ^ rotateRight(x, 16));
        x ^= k4 & (x ^ rotateRight(x, 32));

        return x;
    }

    /**
     * HELPER FUNCTION to convert bit clockwise:
     * 0 0 1            0 0 0  
     * 0 1 0    --->    0 0 0
     * 1 0 0            1 1 1
     * FROM https://www.chessprogramming.org/Flipping_Mirroring_and_Rotating                                              
     * 
     */
    long anticlockwise(long x) {
        long k1 = 0x5555555555555555L;
        long k2 = 0x3333333333333333L;
        long k4 = 0x0f0f0f0f0f0f0f0fL;
        x ^= k1 & (x ^ rotateRight(x, 8));
        x ^= k2 & (x ^ rotateRight(x, 16));
        x ^= k4 & (x ^ rotateRight(x, 32));
        return x;
    }

    /**
     * HELPER FUNCTION to convert bit clockwise:
     * 1 0 0            1 1 1  
     * 1 0 0    --->    0 0 0
     * 1 0 0            0 0 0
     *  FROM https://www.chessprogramming.org/Flipping_Mirroring_and_Rotating                                               
     */
    long colToRow(long x) {
        long t;
        long k1 = 0x5500550055005500L;
        long k2 = 0x3333000033330000L;
        long k4 = 0x0f0f0f0f00000000L;
        t = k4 & (x ^ (x << 28));
        x ^= t ^ (t >> 28);
        t = k2 & (x ^ (x << 14));
        x ^= t ^ (t >> 14);
        t = k1 & (x ^ (x << 7));
        x ^= t ^ (t >> 7);
        return x;
    }

    //For debugging prints out 64 bits as a board
    void printboard(long value) {
        System.out.println();
        System.out.println();
        String binary = String.valueOf(Long.toBinaryString(value)); 

        int count=0;
        String num1 = "1";
        for (int j=0; j<binary.length(); j++){

            if(count==GomokuBoard.COLS){
                count=0;
                System.out.println("");
            }
            if(num1.equalsIgnoreCase(String.valueOf(binary.charAt(j)))){
                System.out.print(" 1 ");
            }else{
                System.out.print(" 0 ");
            }
            count++;
        }
    }
    //refresh timer
    private void refresh() {
        timer = System.currentTimeMillis() + 9000;
    }
    //Check if check if we still have time
    private boolean checkTime() {
        return System.currentTimeMillis() < timer;
    }
    //Check if time has expired
    private void Timer() throws NoTimeException {
        if (!checkTime()) throw new NoTimeException();
    }

    //Class used to store bestMove and score
    class BestMove{
        private long move;
        private int score;      
        public BestMove(){
            score=0;
        }

        public BestMove(int score){
            this.score=score;
        }

        public BestMove(long move, int score){
            this.score=score;
            this.move=move;
        }
    }   

    //Class for exception so we know how much time we have
    private static class NoTimeException extends Throwable { }
}

