package bguspl.set.ex;

import bguspl.set.Env;


import java.util.LinkedList;
import java.util.List;

import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.util.ElementScanner6;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;


/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    protected final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    protected volatile boolean terminate = false;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime ;


    //queue for the player that are waitinf for the dealer's check. 
    private ArrayBlockingQueue<Object[]> checkMe;   

    protected Object lock = new Object();
    //the highest number of score
    protected int maxScore =0; 

    protected Object aiLock = new Object(); 
    //true if the player cant put tokens on the table
    protected volatile boolean needToWait = true; 

   



    



    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        checkMe = new ArrayBlockingQueue<Object[]>(env.config.players,true);
        reshuffleTime = System.currentTimeMillis() +env.config.turnTimeoutMillis;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        //create players threads **************+run
        for (Player p : players) {
            Thread playerThread = new Thread(p); 
            playerThread.start();
        }
        
        while (!shouldFinish()) {
            placeCardsOnTable();
            updateTimerDisplay(true);
            timerLoop();
            removeAllCardsFromTable();
            
        }
        announceWinners();
        terminate();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            
        sleepUntilWokenOrTimeout();
        checkSet();
        updateTimerDisplay(false);   
        placeCardsOnTable();
       
        }
         
    }

    /**
     * Called when the game should be terminated due to an external event.
     * @pre terminate = false & for all the player : player.terminate = false; 
     * @post terminate = true & for all the player : player.terminate = true; 
     */
    public void terminate() {
        // // TODO implement
        for(Player p : players){
            p.terminate();
        }
        terminate = true;       
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    public  void removeCardsFromTable(int[] cards) {
        // TODO implement
        synchronized(table){
        needToWait = true; 
        
        for (Integer c : cards) {
        if(table.cardToSlot[c] != null){
          int idSlot =  table.cardToSlot[c]; //the slot of the card
          for (Player p: players) {          //remove the token of all the players that put on this card
            table.removeToken(p, idSlot);
         }
           
          table.removeCard(idSlot);
       }
    }
        placeCardsOnTable();
    }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        synchronized(table){
        // TODO implement
        Integer[] slotToCard = table.slotToCard; 
        // pass all the slots, if there is a slot whithout a card and we have cards in the deck, we put card in the slot.
        for(int i = 0 ; i<slotToCard.length; i++){
            if(slotToCard[i] == null && deck.size()>0 ){
                Random r = new Random();
                int indexCurrCard = r.nextInt(deck.size()); //choose a random card fron the deck
                int cuurCard = deck.get(indexCurrCard);
                table.placeCard(cuurCard, i);
                deck.remove(indexCurrCard);

            }
            }
            needToWait = false; 
         }
         
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private  void sleepUntilWokenOrTimeout() {
        // TODO implement
       
       try{
        synchronized(this){
            
                wait(10);
        }
        }catch(InterruptedException e){}
       
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    protected void updateTimerDisplay(boolean reset) {
        // TODO implement
        
        if(reset){
           env.ui.setCountdown(env.config.turnTimeoutMillis, false); 
           reshuffleTime = System.currentTimeMillis()+env.config.turnTimeoutMillis;
        }
        else 
           env.ui.setCountdown(Math.max((reshuffleTime- System.currentTimeMillis()),0), false);
        

           }
    

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
      
        // TODO implement
        needToWait = true; 
        Integer [] slots = table.slotToCard;
        for(int i =0; i< slots.length; i++ ){
           //remove all the token's players from this slot
            for( Player p: players){
                table.removeToken(p, i);
            }
            //remove the card from the table and return him to ths deck
            if(slots[i] != null){  
            int currCard = slots[i];
            deck.add(currCard);
            table.removeCard(i);
            }
        }  
      
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        
        LinkedList<Integer> theWinners= new LinkedList<>(); 
        for( int i = 0; i<players.length; i++){
            if(players[i].score() == maxScore)
            theWinners.add(players[i].id);
            
        }
       
        int[] WinnerArray =new int[theWinners.size()];
      
        for(int i = 0; i<WinnerArray.length; i++){
            WinnerArray[i] = theWinners.get(i).intValue();
           
        }
       
        env.ui.announceWinner(WinnerArray);
     
    }


    public void checkSet (){
        Object[] currChecking = null;
        if(checkMe.peek() != null){
        try{
        
        currChecking = checkMe.take();
      
        }catch(InterruptedException e){}
       
        Player p = players[(int)currChecking[0]];
        LinkedList<Integer> setToCheck = (LinkedList<Integer>)currChecking[1];
      
        if(setToCheck.size() == 3){
            int[] cardsToSet = new int[3];
            for( int i = 0; i<3; i++){
                cardsToSet[i] = setToCheck.get(i);
            }

            boolean isSet = env.util.testSet(cardsToSet);

            if(isSet){
            ; 
                if(p.score()+1 > maxScore){
                    maxScore = p.score() +1; 
                }
                try {
                    p.waitingForCheck.put(true);
               
                    }
                    catch(InterruptedException e){}

                removeCardsFromTable(cardsToSet);
                
                updateTimerDisplay(true);

            }

            else{
            
                try {
                    p.waitingForCheck.put(false);
            
                }
                    catch(InterruptedException e){}
            }
    } 

    else{
        try {
            p.waitingForCheck.put(false);
        
        }
            catch(InterruptedException e){}
        }
    }
    
    }

    public void addMeToQueue(Object[] set){
        try{
        checkMe.put(set);
        }catch (InterruptedException e){}
        
        synchronized(this){
            notifyAll();
        }
    
     }

    /**
    * @return - this.maxScore
    */
    public int TheMaxScore(){
        return this.maxScore;
    }
}
