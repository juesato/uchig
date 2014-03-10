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

public class arbkai extends AbstractExchangeArbCase {
     
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
    double aggression;
    double threshold;
    double edge;
    double minSpread;
    int timeStep = 5; //first time this will be incremented is at 5
    int totalTrades; //decent metric of how aggressive we're being
    
    //To find how volatile something is, keep track of the last 20 or so ticks.
    //Goes from most recent to least recent, so index 0 is the most recent one.
    final int numTrackedQuotes = 20;
    ArrayList<Quote> robotTrackedQuotes = new ArrayList<Quote>();
    ArrayList<Quote> snowTrackedQuotes = new ArrayList<Quote>();
    ArrayList<Double> fairPrices = new ArrayList<Double>();

        public void addVariables(IJobSetup setup) {
            // Registers a variable with the system.
            setup.addVariable("aggression", "factor used to adjust something", "double", "0.75");
            setup.addVariable("threshold", "threshold on difference between two exchanges before trading", "double", "1.0");
            setup.addVariable("edge", "small amount of edge", "double", "0.20");
            setup.addVariable("minSpread", "minimum size of spread (for round 3 should be 3 for safety)", "double", "3.0");
        }

        public void initializeAlgo(IDB database) {
            // Databases can be used to store data between rounds
            myDatabase = database;
            
            database.put("currentPosition", 0);
            aggression = getDoubleVar("aggression");
            threshold = getDoubleVar("threshold");
            edge = getDoubleVar("edge");
            minSpread = getDoubleVar("minSpread");
        }

        @Override
        public void fillNotice(Exchange exchange, double price, AlgoSide algoside) {
            log("QUOTE FILLED at a price of " + price + " on " + exchange + " as a " + algoside);
            log("Trade happened at time" + (timeStep-5));
            if(algoside == AlgoSide.ALGOBUY){
                position += 1;
                spent+=price;
  
            }else{
                position -= 1;
                spent-=price;
            }
            totalTrades++;
            log ("TOTAL TRADES " + totalTrades);
            //log ("TIME IS T=" + timeStep);
            
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
            spent -= clearedQuantity*price;
        }

        @Override
        public void newTopOfBook(Quote[] quotes) {
            timeStep++;
            log ("TIME: " + timeStep);
            for (Quote quote : quotes) {
                //log("NEW BID of " + quote.bidPrice + ", and ask of " + quote.askPrice + " from " + quote.exchange);
            }

            double robotMid = (quotes[0].bidPrice+quotes[0].askPrice)/2.0;
            double snowMid = (quotes[1].bidPrice+quotes[1].askPrice)/2.0;

            
            
            fair = (robotMid+snowMid)/2.0;
            
            int size = robotTrackedQuotes.size();
            if (size > 20) {
                robotTrackedQuotes.remove(0);
                snowTrackedQuotes.remove(0);
                fairPrices.remove(0);
            }

            robotTrackedQuotes.add(quotes[0]);
            snowTrackedQuotes.add(quotes[1]);
            fairPrices.add(fair);
            size = robotTrackedQuotes.size();
            
            
             /**
             * Calcuate volatility here based on robotTrackedQuotes and snowTrackedQuotes
             */
            double maxChange = 0.0;
            double currentChange = 0.0;
            for (int i = 0; i <= Math.min(size-6,10) ; i ++) {
                currentChange = fairPrices.get(size-1-i)-fairPrices.get(size-6-i);
                maxChange = Math.max(maxChange,currentChange);
            }
            for (int i = 1; i < Math.min(size,5); i ++) {
                currentChange = fairPrices.get(size-1)-fairPrices.get(size-1-i);
                maxChange = Math.max(maxChange, currentChange*5/i);
            }
            
            double adjustBasedOnPosition = 0.0;
            double positionConstant = 0.10;
            int selfImposedPositionLimit = 160;
            if (timeStep > 750) {
                int helperVar = (timeStep-750)/10;
                selfImposedPositionLimit = 160-helperVar*6;
            }
                
            if (position >= selfImposedPositionLimit) {
                adjustBasedOnPosition = (-position+selfImposedPositionLimit)*positionConstant;
            }
                //log ("ADJUSTING" + adjustBasedOnPosition);
            if (position <= -selfImposedPositionLimit) {
                adjustBasedOnPosition = (-position-selfImposedPositionLimit)*positionConstant;
            }
                //log ("ADJUSTING" + adjustBasedOnPosition); 
            
            double netpnl = fair*position - spent;
            //log ("CURRENT PNL: " + netpnl);
            double dontTrade = 30.00;
            //log ("My fair price bb: " + fair);
            
            //double threshold = 1.0;
            if (Math.abs(robotMid-snowMid) > threshold) {
                log ("WOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOW this is an arbitrage opportunity");
                dontTrade = 0.0;
            }
            
            double adjustedFair = fair + adjustBasedOnPosition;
            double spreadSizeOneDirection = Math.max(aggression*maxChange+edge+dontTrade, minSpread/2);
            
            desiredRobotPrices[0] = adjustedFair-spreadSizeOneDirection;
            desiredRobotPrices[1] = adjustedFair+spreadSizeOneDirection;

            desiredSnowPrices[0] = adjustedFair-spreadSizeOneDirection;
            desiredSnowPrices[1] = adjustedFair+spreadSizeOneDirection;
            
        }

        @Override
        public Quote[] refreshQuotes() {
            Quote[] quotes = new Quote[2];
            quotes[0] = new Quote(Exchange.ROBOT, desiredRobotPrices[0], desiredRobotPrices[1]);
            quotes[1] = new Quote(Exchange.SNOW, desiredSnowPrices[0], desiredSnowPrices[1]);
            log("MY BID IS " + desiredRobotPrices[0] + ", and my ask is " + desiredRobotPrices[1] + " on both exchanges");
            
            return quotes;
        }

    }

    @Override
    public ArbCase getArbCaseImplementation() {
        return new MyArbImplementation();
    }

}
