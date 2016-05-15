import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import static java.awt.event.KeyEvent.*;
import java.io.*;
import javax.sound.sampled.*;

public class ChessGame implements MouseListener, KeyListener{
    //Fields for the pieces, panels, whether it's white's turn, the bullet game, or music is playing, the kings, frame and music player
    private static ArrayList<GamePiece> black, white, removedBlack, removedWhite;
    private static JPanel overall;
    private static ChessPanel panel;
    private static BulletPanel bulletPanel;
    private static boolean whiteTurn, bulletGame, musicOn;
    private static JFrame frame;
    private static King blackKing, whiteKing;
    private static Clip clip;
    private static int whiteTime, blackTime, whiteCollisions, blackCollisions;

    public static void main(String[] args){
        //loop the initial music (Mozart Rondo Alla Turca)
        try{
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new File("Rondo.wav")));
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            musicOn = true;
        }catch (Exception e){
       	    e.printStackTrace();
        }

        //make window
        frame = new JFrame("Ugly Chess");
        frame.setSize(new Dimension(605,420));

        //define lists of black/white pieces
        black = new ArrayList<>();
        white = new ArrayList<>();
        removedBlack = new ArrayList<>();
        removedWhite = new ArrayList<>();

        //add pawns
        for(int i = 0; i < 8; i++){
            black.add(new Pawn(new Point(i,1), false));
            white.add(new Pawn(new Point(i,6), true));
        }

        //instantiate black pieces
        black.add(new Rook(new Point(0,0)));
        black.add(new Rook(new Point(7,0)));
        black.add(new Knight(new Point(1,0)));
        black.add(new Knight(new Point(6,0)));
        black.add(new Bishop(new Point(2,0)));
        black.add(new Bishop(new Point(5,0)));
        black.add(new Queen(new Point(3,0)));
        black.add(new King(new Point(4,0)));

        //instantiate white pieces
        white.add(new Rook(new Point(0,7)));
        white.add(new Rook(new Point(7,7)));
        white.add(new Knight(new Point(1,7)));
        white.add(new Knight(new Point(6,7)));
        white.add(new Bishop(new Point(2,7)));
        white.add(new Bishop(new Point(5,7)));
        white.add(new Queen(new Point(3,7)));
        white.add(new King(new Point(4,7)));

        //instantiate panels
        panel = new ChessPanel(black, white, removedBlack, removedWhite);
        bulletPanel = new BulletPanel(removedBlack, removedWhite, black.get(black.size()-1), white.get(white.size()-1));

        //instantiate cards (for panel switching) and add panel/bulletpanel, showing chess initially
        overall = new JPanel(new CardLayout());
        overall.add(panel, "Chess");
        overall.add(bulletPanel, "Bullet");
        ((CardLayout) overall.getLayout()).show(overall, "Chess");

        //set settings for frame
        frame.setContentPane(overall);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //set to white's turn, add a mouseListener and keylistener (this class implements them)
        whiteTurn = true;
        frame.addMouseListener(new ChessGame());
        frame.addKeyListener(new ChessGame());

        //set times to 0, start timer
        whiteTime = blackTime = 0;
        Timer time = new Timer();
        time.schedule(new IncTime(), 0, 1000);
    }
    private static class IncTime extends TimerTask {//increment time every second, depending on which person is waiting
        public void run(){
            if(whiteTurn)
                whiteTime++;
            else
                blackTime++;
        }
    }

    //Returns the piece at a given point out of a given array list, returning null if none are found
    public static GamePiece findPiece(ArrayList<GamePiece> pieces, Point p){
        for(GamePiece piece: pieces)
            if(piece.getLocation().equals(p))
                return piece;
        return null;
    }
    //event on mousePress
    public void mousePressed(MouseEvent e){
        //get point on chess grid and active piece (highlighted)
        Point gridPoint = new Point(e.getX() / 50, (e.getY() - 25) / 50);
        GamePiece clicked = panel.activePiece();

        //define friend (moving) and foe lists
        ArrayList<GamePiece> friend, foe, foeRM;
        if(whiteTurn) {
            friend = white;
            foe = black;
            foeRM = removedBlack;
        }
        else{
            friend = black;
            foe = white;
            foeRM = removedWhite;
        }

        if(clicked == null){//if none are already clicked, select piece on grid
            clicked = findPiece(friend, gridPoint);

            if (clicked != null) {//if there is a piece at mouse click, activate the piece and set active moves to trimmed set of moves
                panel.activate(clicked);
                panel.setMoves(trimMoves(clicked, friend, foe));
            }
        }
        else{//if there is an active piece, get location and move
            //find if point clicked is on any legal moves
            boolean hasPoint = false;
            for(Point p: panel.getMoves())
                hasPoint = hasPoint || p.equals(gridPoint);

            //if move is legal
            if(hasPoint){
                //find the foe on that move (if any) and remove it, setting time of capture
                for (int x = 0; x < foe.size(); x++)
                    if (foe.get(x).getLocation().equals(gridPoint)) {
                        if(whiteTurn)
                            foe.get(x).setTime(whiteTime);
                        else
                            foe.get(x).setTime(blackTime);

                        foeRM.add(foe.remove(x));
                    }

                //if the move is castling (king moves 2), also move rook
                int y = clicked.getLocation().getY(), x = clicked.getLocation().getX();
                if(clicked.getID().equals("K") && y==gridPoint.getY() && Math.abs(x - gridPoint.getX())==2)
                    if(x > gridPoint.getX())
                        findPiece(friend, new Point(0, y)).setLocation(new Point(3, y));
                    else
                        findPiece(friend, new Point(7, y)).setLocation(new Point(5, y));

                //move the clicked piece to the new location
                clicked.setLocation(gridPoint);

                


                //if the piece was a pawn moved to the edge, replace it with a queen
                if(clicked.getID().equals("P") && (clicked.getLocation().getY() == 0 || clicked.getLocation().getY() == 7)){
                    friend.remove(friend.indexOf(clicked));
                    friend.add(new Queen(clicked.getLocation()));
                }

                //give the turn to the other player
                whiteTurn = !whiteTurn;
            }
            //return board to a neutral (unhighlighted) state
            panel.deactivate();
        }

        //redraw board with new state after clicked
        panel.repaint();
    }

    //trim moves list to only moves that wouldn't put you in check
    private static ArrayList<Point> trimMoves(GamePiece p, ArrayList<GamePiece> friendPieces, ArrayList<GamePiece> foePieces) {
        //get raw moves (before trimming)
        ArrayList<Point> moves = p.getMoves(friendPieces, foePieces);

        GamePiece temp;
        Point origLoc = p.getLocation();
        for (int i = 0; i < moves.size(); i++) {//move clicked piece to all possible locations
            //move clicked piece to location of move
            p.setLocation(moves.get(i));

            //if there is a foe at the potential move, save it in a temporary variable, and remove the foe
            int ind = foePieces.indexOf(findPiece(foePieces, moves.get(i)));
            temp = null;
            if (ind != -1)
                temp = foePieces.remove(ind);

            //if you are still in check, the move is illegal, and is removed from the list
            if (inCheck(friendPieces, foePieces) != null) {
                moves.remove(i);
                i--;
            }
            //if the foe was removed earlier, add it back
            if (ind != -1)
                foePieces.add(temp);
        }
        //reset the clicked piece to its original location after parsing move list
        p.setLocation(origLoc);

        //return all potential moves
        return moves;
    }
    //Returns king if the "friend" is in check, null otherwise
    public static GamePiece inCheck(ArrayList<GamePiece> friendPieces, ArrayList<GamePiece> foePieces){
        ArrayList<Point> enemyMoves = new ArrayList<>();
        for(GamePiece p: foePieces)

            if(p.getID().equals("K")) {//add simple king moves to avoid indirect recursion
                enemyMoves.add(new Point(p.getLocation()).incX(1));
                enemyMoves.add(new Point(p.getLocation()).incX(-1));
                enemyMoves.add(new Point(p.getLocation()).incY(1));
                enemyMoves.add(new Point(p.getLocation()).incY(-1));
                enemyMoves.add(new Point(p.getLocation()).incX(1).incY(1));
                enemyMoves.add(new Point(p.getLocation()).incX(1).incY(-1));
                enemyMoves.add(new Point(p.getLocation()).incX(-1).incY(1));
                enemyMoves.add(new Point(p.getLocation()).incX(-1).incY(-1));
            }
            else//add moves of all pieces to enemyMoves
                enemyMoves.addAll(p.getMoves(foePieces, friendPieces));

        //get king piece
        King k = new King(new Point(0,0));
        for(GamePiece p: friendPieces)
            if(p.getID().equals("K"))
                k = (King) p;

        //if any potential enemy moves kill the king (check), return the king
        for(Point pt: enemyMoves)
            if(pt.equals(k.getLocation()))
                return k;

        //otherwise, return null
        return null;
    }
    //return name of person in checkmate, null otherwise
    public static void checkmate(){
        //add all potential white moves to one list
        ArrayList<Point> moves = new ArrayList<>();
        for (GamePiece p : white)
            moves.addAll(trimMoves(p, white, black));
        //if there are no potential moves, white is in checkmate, remove mouselisteners, start a thread for the bullet game loop (to allow keylisteners)
        if (moves.size() == 0) {
            if(frame.getMouseListeners().length>0)
                frame.removeMouseListener(frame.getMouseListeners()[0]);
            Thread t = new Thread(new BulletThread(removedWhite));
            t.start();
            return;
        }

        //same for black
        moves = new ArrayList<>();
        for (GamePiece p : black)
            moves.addAll(trimMoves(p, black, white));
        if (moves.size() == 0) {
            if(frame.getMouseListeners().length>0)
                frame.removeMouseListener(frame.getMouseListeners()[0]);
            Thread t = new Thread(new BulletThread(removedBlack));
            t.start();
        }
    }
    //class to transition music in its own thread (to ensure smooth playback regardless of any lag)
    private static class ClipThread implements Runnable{
        public void run(){
            try {
                //leave a 1/10 second gap then play the transition music
                clip.stop();
                Thread.sleep(100);
                clip = AudioSystem.getClip();
                clip.open(AudioSystem.getAudioInputStream(new File("Transition.wav")));
                if(musicOn)
                    clip.start();

                //after a predetermined length of the clip, loop the main music indefinitely (The Qemists - Stompbox (Spor Remix))
                Thread.sleep(22200);

                clip = AudioSystem.getClip();
                clip.open(AudioSystem.getAudioInputStream(new File("Stompbox.wav")));
                if(musicOn)
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    private static ArrayList<GamePiece> doublePieces(ArrayList<GamePiece> original){ 
        ArrayList<GamePiece> newPieces = new ArrayList<>();
        for (GamePiece p : original)
            switch (p.getID()) {
                case "P":
                    newPieces.add(new Pawn(new Point(0, 0), true).setTime(p.getTime()));
                    break;
                case "R":
                    newPieces.add(new Rook(new Point(0, 0)).setTime(p.getTime()));
                    break;
                case "B":
                    newPieces.add(new Bishop(new Point(0, 0)).setTime(p.getTime()));
                    break;
                case "N":
                    newPieces.add(new Knight(new Point(0, 0)).setTime(p.getTime()));
                    break;
                case "Q":
                    newPieces.add(new Queen(new Point(0, 0)).setTime(p.getTime()));
                    break;
            }

        return newPieces;
    }       
    private static String formatTime(int seconds){
        if(seconds <= 0)
            return "0.00.000";

        return seconds/60000 + "." + String.format("%02d", (seconds/1000)%60) + "." + String.format("%03d", seconds%1000);
    } 
    private static class BulletThread implements Runnable{//calls game loop in new thread based on checkmate
        ArrayList<GamePiece> addTo;
        public BulletThread(ArrayList<GamePiece> removed){addTo = removed;}
        public void run() {//creates and executes the entire bullet game
            //set field for current bullet game execution
            bulletGame = true;

            //find black king piece, set it to field
            for (GamePiece p : black)
                if (p.getID().equals("K"))
                    blackKing = (King) p;

            //find white king piece, set it to field
            for (GamePiece p : white)
                if (p.getID().equals("K"))
                    whiteKing = (King) p;

            //add all new pieces to the recipient arraylist
            addTo.addAll(doublePieces(addTo));

            //instantiate canvas (on which the bullet game will be drawn)
            Canvas canvas = new Canvas();
            canvas.setIgnoreRepaint(true);
            canvas.setSize(600, 400);

            //add canvas to card layout and show it
            overall.add(canvas, "Canvas");
            
	        //get buffer strategy (the canvas will be double buffered)
            canvas.createBufferStrategy(2);
            BufferStrategy buffer = canvas.getBufferStrategy();

            
            //start thread to facilitate music transition
	        Thread music = new Thread(new ClipThread());
            music.start();
	    
	        //set the canvas background to mimic chess board, paint initial grid, and show the canvas
            canvas.setBackground(Color.WHITE);
            Graphics graphics = buffer.getDrawGraphics();
            graphics.setColor(new Color(255,225,175));
            graphics.fillRect(0,0,1000,1000);
            panel.paintAll(graphics);
            if(!buffer.contentsLost())
                buffer.show();
            ((CardLayout) overall.getLayout()).show(overall, "Canvas");
                
            long startTime = System.nanoTime();
            //begin transition (fade to burnt sienna) over a measure, using a buffer (sleeping the time between when it should be and the current time in order to avoid lag ruining transition time)
            for(double i = 1; i <= 255; i+=255.0/100) {
                graphics = buffer.getDrawGraphics();
                graphics.setColor(new Color(255,225,175));
                graphics.fillRect(0,0,1000,1000);
                panel.paintAll(graphics);

                //fade background progressively to burnt sienna (grid color) through increasing alpha (opacity) values
                graphics.setColor(new Color(153,51,0,(int) i));
                graphics.fillRect(0,0,1000,1000);
                if (!buffer.contentsLost())
                    buffer.show();
                try{Thread.sleep((long) ((startTime+285000000L/255.0*i - System.nanoTime())/1000000.0));}catch(Exception e){}
            }

            //fade to black using the same strategy (with a variable-alpha black color over opaque burnt sienna) over a measure, further buffered to mitigate any leftover lag from the first loop
            for(int i = 1; i <= 255; i++){
                graphics = buffer.getDrawGraphics();

                graphics.setColor(new Color(153,51,0));
                graphics.fillRect(0,0,1000,1000);
                graphics.setColor(new Color(0,0,0,i));
                graphics.fillRect(0,0,1000,1000);
                if (!buffer.contentsLost())
                    buffer.show();
                try{Thread.sleep((long) ((startTime+2850000000L+2971000000L/255.0*i - System.nanoTime())/1000000.0));}catch(Exception e){}
            }
            //fade one side to white the same way over one measure, unbuffered due to the low lag potential
            for(int i = 1; i <= 255; i++){
                graphics = buffer.getDrawGraphics();

                graphics.setColor(new Color(i,i,i));
                graphics.fillRect(0,0,300,450);
                if (!buffer.contentsLost())
                    buffer.show();
                try{Thread.sleep(10);}catch(Exception e){}
            }

            //define dialogue per side (to easily reference)
            String[] leftDialogue = {"White", "Control with WASD", "Pieces: " + String.format("%02d", removedWhite.size()), "Time: " + formatTime(bulletPanel.blackTime())};
            String[] rightDialogue = {"Black", "Control with Arrows", "Pieces: " + String.format("%02d", removedBlack.size()), "Time: " + formatTime(bulletPanel.whiteTime())};

            //set text antialiasing
            Graphics2D graph = (Graphics2D) graphics;
            graph.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            //iterate through and paint the dialogue according to index (in the color of the king), first showing the king pieces in an appropriate location
            //adds dialogue once per measure
            for(int i = -1; i < leftDialogue.length; i++){
                graph.setColor(Color.RED);
                switch(i){
                    case 3:
                        graph.setFont(new Font("Arial", Font.PLAIN, 30));
                        graph.drawString(rightDialogue[3], 355, 325);
                        break;
                    case 2:
                        graph.setFont(new Font("Arial", Font.PLAIN, 30));
                        graph.drawString(rightDialogue[2], 385, 275);
                        break;
                    case 1:
                        graph.setFont(new Font("Arial", Font.PLAIN, 30));
                        graph.drawString(rightDialogue[1], 325, 150);
                        break;
                    case 0:
                        graph.setFont(new Font("Arial", Font.BOLD, 50));
                        graph.drawString(rightDialogue[0], 390, 75);
                        break;
                    case -1:
                        blackKing.setLocation(new Point(450, 100));
                        BulletPanel.paintPiece(graph, blackKing, Color.WHITE, Color.RED);
                        break;
                }

                graph.setColor(Color.CYAN);
                switch(i){
                    case 3:
                        graph.setFont(new Font("Arial", Font.PLAIN, 30));
                        graph.drawString(leftDialogue[3], 45, 325);
                        break;
                    case 2:
                        graph.setFont(new Font("Arial", Font.PLAIN, 30));
                        graph.drawString(leftDialogue[2], 75, 275);
                        break;
                    case 1:
                        graph.setFont(new Font("Arial", Font.BOLD, 30));
                        graph.drawString(leftDialogue[1], 15, 150);
                        break;
                    case 0:
                        graph.setFont(new Font("Arial", Font.BOLD, 50));
                        graph.drawString(leftDialogue[0], 70, 75);
                        break;
                    case -1:
                        whiteKing.setLocation(new Point(140, 100));
                        BulletPanel.paintPiece(graph, whiteKing, Color.BLACK, Color.CYAN);
                        break;
                }
                if (!buffer.contentsLost())
                    buffer.show();
                try{Thread.sleep(2791);}catch(Exception e){}
            }

            //assorted declarations and initializations for the game loop
            final double nanoPerTics = 1000000000 / 60;
            final int max = 5;
            final long timePerDub = 10000000000L;
            double last = System.nanoTime()-nanoPerTics, now = System.nanoTime(), FPSTime=0, FPS=0, lastDub=0;
            int updates, frames=0, blackTime, whiteTime;
            boolean running = true, suddenDeath = false;

            //game loop; updates the logic and renders the game as long as one side is still running, according to a particular game speed, limiting frame rate
            while (running || suddenDeath){
                try {
                    //increment the time since the last frame rate calculation
                    FPSTime += now - last;

                    whiteTime = bulletPanel.whiteTime();
                    blackTime = bulletPanel.blackTime();

                    //update the game logic, allowing "catch-up" (through multiple iterations of the update) of up to 5 frames, and adding any collisions to counters
                    updates = 0;
                    while (now - last >= nanoPerTics && updates < max) {
                        bulletPanel.updateGame(60, suddenDeath);
                        if (bulletPanel.white().size() > 1 && (suddenDeath || whiteTime>0))
                            whiteCollisions += collide(whiteKing, bulletPanel.white(), 25, 265, 25, 365);
                        if (bulletPanel.black().size() > 1 && (suddenDeath || blackTime>0))
                            blackCollisions += collide(blackKing, bulletPanel.black(), 325, 565, 25, 365);
                        last += nanoPerTics;
                        updates++;
                        frames++;
                    }
                    if (now - last > nanoPerTics) {
                        last = now - nanoPerTics;
                    }

                    //render the game
                    render(graphics, buffer, FPS, blackTime, whiteTime);
                    
                    //after at least a second, update the FPS counter by dividing frames by the time it took to update them (parenthesis to prevent overflow)
                    if (FPSTime > 1000000000) {
                        FPS = frames * (1000000000 / FPSTime);
                        FPSTime = 0;
                        frames = 0;
                    }

                    //show the changes
                    if (!buffer.contentsLost())
                        buffer.show();

                    //check to see if the loop should continue running based on if any time is left
                    running = running && (whiteTime>0 || blackTime>0);
                    suddenDeath = suddenDeath && whiteCollisions == blackCollisions;
                    
                    if(!running && !suddenDeath && whiteCollisions == blackCollisions){
                        suddenDeath = true;
                        clip.stop();
                        for(int i = 0; i < 30; i++){
                            render(graphics, buffer, FPS, whiteTime, blackTime);
                            if(i%2 == 0){
                                graphics.drawString("SUDDEN DEATH", 100, 200);
                                graphics.setColor(new Color((int) (Math.random()*255),(int) (Math.random()*255), (int) (Math.random()*255)));
                                graphics.setFont(new Font("Arial", Font.BOLD, 50));
                            }
                            if(!buffer.contentsLost())
                                buffer.show();
                            try{Thread.sleep(100);}catch(Exception e){e.printStackTrace();}
                            lastDub = System.nanoTime();
                        }
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                    }
                    if(suddenDeath && now-lastDub > timePerDub){
                        ArrayList<GamePiece> newBlack = doublePieces(bulletPanel.black());
                        ArrayList<GamePiece> newWhite = doublePieces(bulletPanel.white());

                        for(GamePiece p: newBlack)
                            p.randomize(325,565,25,365);
                        for(GamePiece p: newWhite)
                            p.randomize(20,265,25,365);

                        bulletPanel.black().addAll(newBlack);
                        bulletPanel.white().addAll(newWhite);
                        lastDub = now;
                    }

                    //yield to other threads at least once, until it has been at least the specified time between tics
                    do {
                        Thread.yield();
                        now = System.nanoTime();
                    } while (now - last < nanoPerTics);
                } finally {}
            }

            //designate the winner
            if(whiteCollisions < blackCollisions)
                winner(graphics, buffer, "White", whiteCollisions, blackCollisions, 300, Color.WHITE, Color.BLACK);
            else
                winner(graphics, buffer, "Black", blackCollisions, whiteCollisions, 0, Color.BLACK, Color.WHITE);
        }
    }
    private static void render(Graphics graphics, BufferStrategy buffer, Double FPS, int whiteTime, int blackTime){
        //add the pieces painting and other parts of bulletPanel-based painting
        graphics = buffer.getDrawGraphics();
        bulletPanel.paintComponent(graphics);

        //set the font
        graphics.setFont(new Font("Serif", Font.ITALIC, 10));

        //add the black text for pieces left, collisions, time, and FPS
        graphics.setColor(Color.BLACK);
        graphics.drawString("Pieces left: " + String.format("%02d", removedWhite.size()) + ".  Collisions: " + String.format("%02d", whiteCollisions) + ". Time: " + formatTime(whiteTime)+".", 25, 20);
        graphics.drawString("FPS: " + String.format("%.1f", FPS), 25, 390);

        //add the white text for pieces left, collisions, and time
        graphics.setColor(Color.WHITE);
        graphics.drawString("Pieces left: " + String.format("%02d", removedBlack.size()) + ".  Collisions: " + String.format("%02d", blackCollisions) + ". Time: " + formatTime(blackTime) + ".", 349, 20);
    }
    private static void winner(Graphics graphics, BufferStrategy buffer, String winnerName, int winnerCollisions, int loserCollisions, int leftEdge, Color box, Color text){
        //fade losing side to color of winning side
        for(int i = 1; i <= 255; i++){
            render(graphics, buffer, 60.0, 0, 0);

            graphics.setColor(new Color(box.getRed(), box.getGreen(), box.getBlue(), i));
            graphics.fillRect(leftEdge, 0, 310, 500);

            if (!buffer.contentsLost())
                buffer.show();
        }

        //paint the text for the winner, and show
        graphics.setColor(text);
        graphics.setFont(new Font("Arial", Font.BOLD, 75));
        graphics.drawString(winnerName, leftEdge+40, 80);
        graphics.drawString("Won.", leftEdge+40, 160);
        graphics.setFont(new Font("Arial", Font.BOLD, 50));
        graphics.drawString("(" + winnerCollisions + "-" + loserCollisions + ")", leftEdge+50, 360);

	//show changes
        if (!buffer.contentsLost())
            buffer.show();
    }

    //return the number of collisions between the king and foe pieces, randomizing the locations of any collided particles
    private static int collide(King k, ArrayList<GamePiece> foePieces, int xmin, int xmax, int ymin, int ymax){
        int count = 0;
        Point loc = k.getLocation();
        for(GamePiece p: foePieces) {
            Point ploc = p.getLocation();
            //p has collided with the king if the boxes of the king and p's heights and radii with their respective upper left-hand corner coordinates intersect
            if (!p.getID().equals("K") && ((ploc.getX() > loc.getX() && ploc.getX()-loc.getX() < k.getWidth()) || (loc.getX() > ploc.getX() && loc.getX()-ploc.getX()< p.getWidth())) && ((ploc.getY() > loc.getY() && ploc.getY()-loc.getY() < k.getHeight()) || (loc.getY() > ploc.getY() &&loc.getY()-ploc.getY()< p.getHeight()))) {
                p.randomize(xmin, xmax, ymin, ymax);
                count++;
            }
        }
        return count;
    }

    //on keypress, set a king field determining the direction with respect to either X or Y
    public void keyPressed(KeyEvent e) {
        if (bulletGame) {
            switch (e.getKeyCode()) {
                case VK_W:
                    whiteKing.setIncY(-1);
                    break;
                case VK_A:
                    whiteKing.setIncX(-1);
                    break;
                case VK_S:
                    whiteKing.setIncY(1);
                    break;
                case VK_D:
                    whiteKing.setIncX(1);
                    break;
                case VK_UP:
                    blackKing.setIncY(-1);
                    break;
                case VK_LEFT:
                    blackKing.setIncX(-1);
                    break;
                case VK_DOWN:
                    blackKing.setIncY(1);
                    break;
                case VK_RIGHT:
                    blackKing.setIncX(1);
                    break;
            }
        }
    }
    //on the release of a key, reset the corresponding Y/X increments to zero, ending movement in that direction until keypress again
    public void keyReleased(KeyEvent e){
        if(bulletGame){
            switch(e.getKeyCode()){
                case VK_W:
                case VK_S:
                    whiteKing.setIncY(0);
                    break;
                case VK_A:
                case VK_D:
                    whiteKing.setIncX(0);
                    break;
                case VK_UP:
                case VK_DOWN:
                    blackKing.setIncY(0);
                    break;
                case VK_LEFT:
                case VK_RIGHT:
                    blackKing.setIncX(0);
                    break;
            }
        }
    }
    //if an M is typed, invert the mute status (music->silence and vice versa)
    public void keyTyped(KeyEvent e){
        if (e.getKeyChar() == 'm') {
            if (musicOn)
                clip.stop();
            else
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            musicOn = !musicOn;
        }

    }

    //method declarations for unused listener functions
    public void mouseReleased(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mouseClicked(MouseEvent e){}
}
