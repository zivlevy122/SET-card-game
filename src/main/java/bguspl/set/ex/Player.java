package bguspl.set.ex;

//import java.util.Queue;
import java.util.logging.Level;

import javax.swing.plaf.synth.SynthViewportUI;

//import javax.sql.rowset.JoinRowSet;

import bguspl.set.Env;

import java.util.LinkedList;//

import  java.util.concurrent.LinkedBlockingQueue ;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    protected volatile boolean terminate;

    /**
     * The current score of the player.
     */
    protected int score;





    /**
     * the card that the player will send to the dealer's check
     */
    protected volatile LinkedList<Integer> cardsIchoose;

    /*
    * on this cards the player click
    */
    protected LinkedBlockingQueue<Integer> keyQueue;

    /*
    the dealer of the game */
    private Dealer dealer; 

    /*indicating if the set the pleyer send is legal.
     * 1=set, 0=no set, -1= don't know yet
    */
    protected volatile boolean wasPunish = false;
    protected volatile boolean wasChecked;

    protected Object lockUntilCheck = new Object();
    
    protected volatile int legalSET = -1; 
    
    private long freez = 0; 
    protected volatile ArrayBlockingQueue<Boolean> waitingForCheck = new ArrayBlockingQueue<>(1);

    

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        keyQueue = new LinkedBlockingQueue<Integer>(3) ;
        cardsIchoose = new LinkedList<Integer>();
        score = 0;
        this.dealer = dealer; 
         
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();
        
        while (!terminate) {
            // TODO implement main player loop
           
            while( cardsIchoose.size() < 3 |(cardsIchoose.size() == 3 & wasPunish)){
                theNextAct();
            }
            
            // send the set to the dealer to check 
            if(!wasPunish){
                Object[] sentToCheck = new Object[2]; 
                sentToCheck[0] = id; 
                sentToCheck[1] = cardsIchoose; 
                dealer.addMeToQueue(sentToCheck); 
                Boolean isLegal = null;
                try{
                isLegal = waitingForCheck.take(); 
                }catch(InterruptedException e){}

                if(isLegal) point();
                else if(!isLegal & cardsIchoose.size()==3) penalty();

                 
            }
           
           }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            
            while (!terminate)  {
                // TODO implement player key press simulator
                aiThread = Thread.currentThread(); 
                Random r = new Random();
                int s = r.nextInt(env.config.tableSize);
                keyPressed(s);
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     * 
     * @pre - terminate = false
     * @post - terminate = true
     */

    public void terminate() {
        // TODO implement
        
        terminate = true; 
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public  void keyPressed(int slot) {
        // TODO implement
       if(!dealer.needToWait){
        if(table.slotToCard[slot]!= null){
        int whichCard = table.slotToCard[slot]; 
       
        try{
            keyQueue.put(whichCard);
        }
        catch(InterruptedException e){
           
        }
         } }
    }

     /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public  void point() {
        // TODO implement

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        freez = env.config.pointFreezeMillis/1000;
        for(long i=0; i< freez; i++){
            
          
            env.ui.setFreeze(id, env.config.pointFreezeMillis-(i*1000));
            try {
                playerThread.sleep(1000);
                dealer.updateTimerDisplay(false);
            } catch(InterruptedException e) {}
        }
        env.ui.setFreeze(id, 0);
        
   
        
        wasPunish = false;
        freez = 0; 
        keyQueue.clear();;
        
        
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public  void penalty() {
        // TODO implement
       
        freez =env.config.penaltyFreezeMillis/1000;         
         for(long i=0; i< freez; i++){
            env.ui.setFreeze(id, env.config.penaltyFreezeMillis-(i*1000));
     
            try {
                playerThread.sleep(1000);
                dealer.updateTimerDisplay(false);
            } catch(InterruptedException e) {}
    }
     
        env.ui.setFreeze(id, 0);
           
        wasPunish = true;
        freez = 0; 
        keyQueue.clear();;
    }

   

    // insert or remove the first slot(from the queue) the player choose
    public  void theNextAct(){
        if(!keyQueue.isEmpty()){
         int currCard = keyQueue.remove();
         if(table.cardToSlot[currCard] != null){
         int whichSlot = table.cardToSlot[currCard];
         if (cardsIchoose.contains(currCard)){
            table.removeToken(this, whichSlot);
            wasPunish = false; 
         }
         else if (cardsIchoose.size() < 3){
            cardsIchoose.add(currCard);
            table.placeToken(id, whichSlot);
            wasPunish = false; 
         }
          else wasPunish = true;
        }
    }}

    
    /**
    * @return - the player's score
    */
    public int score(){
        return score; 
    }


}
