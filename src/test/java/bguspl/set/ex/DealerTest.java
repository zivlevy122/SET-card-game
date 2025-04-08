package bguspl.set.ex;


import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ArrayBlockingQueue;

import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class DealerTest {
    Dealer dealer;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
   
    private Table table;
    @Mock
    private Logger logger;
    @Mock
    protected ArrayBlockingQueue<Object[]> checkMe; 
    
    protected Player[] players;
   
    private Env env;

    protected volatile boolean terminate;
    private int maxScore =0; 

    
    private List<Integer> deck;


    @BeforeEach
    public void setUp() {
        env = new Env(logger, new Config(logger, ""), ui, util);
        players = new Player[] { new Player(env, dealer, table, 0, false), new Player(env, dealer, table, 1, false) };
        dealer = new Dealer(env, table, players);
        terminate = false;
        table = new Table(env);
    }

    @Test
    void terminate() {
       
        // the expected value for terminate for all players and dealer
        boolean expectedTer = true;

        // call the method we are testing
        dealer.terminate();

        // check that the value for terminate was changed correctly
        assertEquals(expectedTer, dealer.terminate);

        // check that the value for players was changed correctly
        for(int i = 0; i< dealer.players.length; i++){
            Player p = dealer.players[i];
            assertEquals(expectedTer, p.terminate);
        }
    }

    @Test
    void TheMaxScore(){
    // the expected value for TheMaxScore() 
    int expect =dealer.maxScore;

        players[0].score=0;
        players[1].score=0;

    // call the method we are testing
        dealer.TheMaxScore();

        // check that the score was increased correctly
        assertEquals( expect,dealer.TheMaxScore());

    }

    



}