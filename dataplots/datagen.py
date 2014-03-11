import numpy as np
f = open('output3.txt','w')

for repeat1000 in range(100):
    myRound = 3
    if myRound == 1:
        # Round 1
        underlying_vol = 0.3
        shock_std = 0.5
        spread_mean = 1.0
        spread_std = 0.5
        p1 = 0.5
        ex_mem = 0.1
    elif myRound == 2:
        # Round 2
        underlying_vol = 1.0
        shock_std = 0.5
        spread_mean = 2.0
        spread_std = 0.5
        p1 = 0.5
        ex_mem = 0.1
    elif myRound == 3:
        # Round 3
        underlying_vol = 0.2
        shock_std = 0.2
        spread_mean = 0.2
        spread_std = 0.2
        p1 = 0.5
        ex_mem = 0.225

    # 0. Time periods
    T = 1000

    ## A. Generate Book Updates

    # 1. Evolving True Value

    # AR(1) Process
    value = np.zeros(T)
    valuee = np.random.normal(0, underlying_vol, T)
    value[0] = 100.
    for i in xrange(1, T):
        value[i] = value[i-1] + valuee[i]

    # 2. Evolving Deviations from the True Value
    dev1 = np.zeros(T)
    deve1 = np.random.normal(0, shock_std, T)
    dev2 = np.zeros(T)
    deve2 = np.random.normal(0, shock_std, T)

    dev1[0] = 0
    dev2[0] = 0

    for i in xrange(1, T):
        if i > 10:
            dev1[i] = 1.0 * ( deve1[i] + ex_mem * np.sum(np.multiply(np.arange(0.9,0,-0.1),dev1[i+np.arange(-10,-1)])))
            dev2[i] = 1.0 * ( deve2[i] + ex_mem * np.sum(np.multiply(np.arange(0.9,0,-0.1),dev2[i+np.arange(-10,-1)])))
            #if we assume dev1[i] is roughly 1 stddev away at all times, we can get 4.5 *
            #shock is .5 or .2, so deviation from mean can jump around $1.1*shock_std on average from shocks, so about
            #the other component causes deviation of about .8*shock_std * 4.5 * ex_mem - ~about .2 total on first 2 rounds, and about 1.6 on third
        else:
            dev1[i] = 1.0 * ( deve1[i] + 0.75 * deve1[i-1] +  0.5 * deve1[i-2])
            dev2[i] = 1.0 * ( deve2[i] + 0.75 * deve2[i-1] +  0.5 * deve1[i-2])

    ex1 = value + dev1
    ex2 = value + dev2

    # 3. Evolving Spreads
    # (spreads widen when midpoint moves)
    spread1 = np.ones(T)
    spreade1 = np.random.normal(spread_mean, spread_std, T)
    spread2 = np.ones(T)
    spreade2 = np.random.normal(spread_mean, spread_std, T)

    for i in xrange(2,T):
        if i > 5:
            spread1[i] = 0.0 + p1 * np.sum(np.abs(np.diff(ex1[i-5:i]))) \
                             + np.abs(spreade1[i])
            spread2[i] = 0.0 + p1 * np.sum(np.abs(np.diff(ex2[i-5:i]))) \
                             + np.abs(spreade2[i])
        else:
            spread1[i] = 0.0 + np.abs(spreade1[i])
            spread2[i] = 0.0 + np.abs(spreade2[i])


    ex1b = ex1 - spread1
    ex1a = ex1 + spread1

    ex2b = ex2 - spread2
    ex2a = ex2 + spread2



    # 4. Round to nearest 0.25
    roundQtr = lambda x: [round(i / 0.25) * 0.25 for i in x]

    ex1b = roundQtr(ex1b)
    ex1a = roundQtr(ex1a)
    ex2a = roundQtr(ex2a)
    ex2b = roundQtr(ex2b)

    grid = np.array(range(T))




    # f.write("MAX OF EX1B")
    a1 = str(min(ex1b))
    a2 = str(max(ex1b))
    a3 = str(min(ex1a))
    a4 = str(max(ex1a))
    #min bid, max bid, min offer, max offer
    f.write(a1 + ',' + a2 + ',' + a3 + ',' + a4 + '\n')




    ## B. Generate Customer Orders

    # Variance of Customer Order Price
    # Moving Average Process

    custVar = np.zeros(T)
    custVare = np.random.normal(0, 0.5, T)
    custVar[0] = 1.
    for i in xrange(1, T):
        custVar[i] = abs(custVare[i-1] + custVare[i])

    # Customer Orders come in around true value

    # 0 = Bid, 1 = Ask
    custside = np.random.randint(2, size=T)

    # 0 = ROBOT, 1 = SNOW
    custexch = np.random.randint(2, size=T)

    custPrice = [np.random.normal(value[i], custVar[i]) for i in xrange(T)]
    custPrice = roundQtr(custPrice)

    # Clear all customer orders at 0 mod 5 times
    for i in range(0, T, 5):
        custside[i] = -1
        custexch[i] = -1
        custPrice[i] = 0

"""
Exchange book prices:
    ex1a, ex2a (Asks)
    ex1b, ex2b (Bids)

Customer:
    custside, custexch, custPrice
"""
f.close()