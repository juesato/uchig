

import org.chicago.cases.AbstractExchangeArbCase;
import org.chicago.cases.AbstractExchangeArbCase.AlgoSide;
import org.chicago.cases.AbstractExchangeArbCase.ArbCase;
import org.chicago.cases.arb.Quote;

import com.optionscity.freeway.api.IDB;
import com.optionscity.freeway.api.IJobSetup;

public class arbzero extends AbstractExchangeArbCase implements ArbCase {
    
        
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


    public void addVariables(IJobSetup setup) {
        // Registers a variable with the system.
        setup.addVariable("someFactor", "factor used to adjust something", "int", "2");
    }

    public void initializeAlgo(IDB database) {
        // Databases can be used to store data between rounds
        myDatabase = database;
        
        // helper method for accessing declared variables
        factor = getIntVar("someFactor"); 
    }


    public void fillNotice(Exchange exchange, double price, AlgoSide algoside) {
        log("QUOTE FILLED at a price of " + price + " on " + exchange + " as a " + algoside);
        if(algoside == AlgoSide.ALGOBUY){
            position += 1;
            spent+=price;
        }else{
            position -= 1;
            spent-=price;
        }
        pnl = position*fair-spent;
        if (position == 0) {
            log("POSITION 0! " + pnl);
        }
        else {
            log("CURRENT NET PNL IS: " + pnl + " Position: " + position);
        }
    }


    public void positionPenalty(int clearedQuantity, double price) {
        log("I received a position penalty with " + clearedQuantity + " positions cleared at " + price);
        position -= clearedQuantity;
        spent -= clearedQuantity*price;
    }

    public void newTopOfBook(Quote[] quotes) {
        for (Quote quote : quotes) {
            log("I received a new bid of " + quote.bidPrice + ", and ask of " + quote.askPrice + " from " + quote.exchange);
        }

        double robotMid = (quotes[0].bidPrice+quotes[0].askPrice)/2.0;
        double snowMid = (quotes[1].bidPrice+quotes[1].askPrice)/2.0;
        
        fair = (robotMid+snowMid)/2.0;
        
        double netpnl = fair*position - spent;
        log ("PNL: " + netpnl + " Position: " + position);

        desiredRobotPrices[0] = 80.0;
        desiredRobotPrices[1] = 110.0;
        log ("Bid at " + desiredRobotPrices[0] + " and ask of " + desiredRobotPrices[1]);
        desiredSnowPrices[0] = 80.0;
        desiredSnowPrices[1] = 110.0;
    }


    public Quote[] refreshQuotes() {
        Quote[] quotes = new Quote[2];
        quotes[0] = new Quote(Exchange.ROBOT, desiredRobotPrices[0], desiredRobotPrices[1]);
        quotes[1] = new Quote(Exchange.SNOW, desiredSnowPrices[0], desiredSnowPrices[1]);
        return quotes;
    }


    public ArbCase getArbCaseImplementation() {
        return this;
    }

}
