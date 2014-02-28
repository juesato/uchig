/**
 * Ideas:
 * Don't trade early on (or make really wide markets) before you can estimate volatility
 * Based on estimated volatility, make spread around fair price
 * Have different fair prices based on which exchange it is?
 * 
 */

import java.util.ArrayList;

import org.chicago.cases.AbstractExchangeArbCase;
import org.chicago.cases.arb.Quote;

import com.optionscity.freeway.api.IDB;
import com.optionscity.freeway.api.IJobSetup;

public class ArbCaseImplementation extends AbstractExchangeArbCase {
     
    class MyArbImplementation implements ArbCase {
        

    // Note...the IDB will be used to save data to the hard drive and access it later
    // This will be useful for retrieving data between rounds
    private IDB myDatabase;
    int factor;

    int position;
    double[] desiredRobotPrices = new double[2];
    double[] desiredSnowPrices = new double[2];
    double pnl;
    double spent;
    double fair;
    
    int timeStep = 5; //first time this will be incremented is at 5
    int totalTrades; //decent metric of how aggressive we're being
    
    //To find how volatile something is, keep track of the last 20 or so ticks.
    //Goes from most recent to least recent, so index 0 is the most recent one.
    final int numTrackedQuotes = 20;
    ArrayList<Quote> robotTrackedQuotes = new ArrayList<Quote>();
    ArrayList<Quote> snowTrackedQuotes = new ArrayList<Quote>();
    
        public void addVariables(IJobSetup setup) {
            // Registers a variable with the system.
            setup.addVariable("someFactor", "factor used to adjust something", "int", "2");
        }

        public void initializeAlgo(IDB database) {
            // Databases can be used to store data between rounds
            myDatabase = database;
            
            database.put("currentPosition", 10);
        }

        @Override
        public void fillNotice(Exchange exchange, double price, AlgoSide algoside) {
            log("My quote was filled with at a price of " + price + " on " + exchange + " as a " + algoside);
             /**
             * Do I have to include something here tracking my pnl
             */
            if(algoside == AlgoSide.ALGOBUY){
                position += 1;
                spent+=price;
  
            }else{
                position -= 1;
                spent-=price;
            }
            totalTrades++;
            log ("TOTAL TRADES " + totalTrades);
            log ("TIME IS T=" + timeStep);
            
            pnl = position*fair-spent;
            if (position == 0) {
                log("POSITION 0! " + pnl);
            }
            else {
                log("CURRENT NET PNL IS: " + pnl + " Position: " + position);
            }
        }

        @Override
        public void positionPenalty(int clearedQuantity, double price) {
            log("I received a position penalty with " + clearedQuantity + " positions cleared at " + price);
            position -= clearedQuantity;
            /**
             * I have to implement something here too right?
             */
            spent -= clearedQuantity*price;
        }

        @Override
        public void newTopOfBook(Quote[] quotes) {
        	timeStep++;
        	log ("Time: " + timeStep);
            for (Quote quote : quotes) {
                log("I received a new bid of " + quote.bidPrice + ", and ask of " + quote.askPrice + " from " + quote.exchange);
            }
            /**
             * Important part is here I think? Do I have to change the other methods at all
             */
            int size = robotTrackedQuotes.size();
            if (size > 20) {
                robotTrackedQuotes.remove(size-1);
                snowTrackedQuotes.remove(size-1);
            }

            robotTrackedQuotes.add(quotes[0]);
            snowTrackedQuotes.add(quotes[1]);
            
            double robotMid = (quotes[0].bidPrice+quotes[0].askPrice)/2.0;
            double snowMid = (quotes[1].bidPrice+quotes[1].askPrice)/2.0;
            double vol = 0.0;
            /**
             * Calcuate volatility here based on robotTrackedQuotes and snowTrackedQuotes
             */
            
            fair = (robotMid+snowMid)/2.0;
            double netpnl = fair*position - spent;
        	log ("CURRENT PNL: " + netpnl);

            desiredRobotPrices[0] = fair-4.0;
            desiredRobotPrices[1] = fair+4.0;

            desiredSnowPrices[0] = fair-4.0;
            desiredSnowPrices[1] = fair+4.0;
            log("My bid is " + desiredRobotPrices[0] + ", and my ask is " + desiredRobotPrices[1] + " on both exchanges");
        }

        @Override
        public Quote[] refreshQuotes() {
            Quote[] quotes = new Quote[2];
            quotes[0] = new Quote(Exchange.ROBOT, desiredRobotPrices[0], desiredRobotPrices[1]);
            quotes[1] = new Quote(Exchange.SNOW, desiredSnowPrices[0], desiredSnowPrices[1]);
            return quotes;
        }

    }

    @Override
    public ArbCase getArbCaseImplementation() {
        return new MyArbImplementation();
    }

}
