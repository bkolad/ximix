/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.common.crypto.threshold;

import java.math.BigInteger;

/**
 * A basic split secret for a BigInteger value.
 */
public class SplitSecret
{
    private final BigInteger[] coefficients;
    private final BigInteger[] shares;

    /**
     * Base constructor.
     *
     * @param shares the shares the initial secret has been split into.
     * @param coefficients the coefficients associated with the shares.
     */
    public SplitSecret(BigInteger[] shares, BigInteger[] coefficients)
    {
        this.shares = shares;
        this.coefficients = coefficients;
    }

    public BigInteger[] getCoefficients()
    {
        return coefficients;
    }

    public BigInteger[] getShares()
    {
        return shares;
    }
}
