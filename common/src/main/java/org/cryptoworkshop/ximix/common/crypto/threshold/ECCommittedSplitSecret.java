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

import org.bouncycastle.math.ec.ECPoint;

/**
 * Holding class for a split secret for an Elliptic Curve algorithm,
 */
public class ECCommittedSplitSecret
    extends SplitSecret
{
    private final ECPoint[] commitments;
    private final BigInteger[] witnesses;

    /**
     * Base constructor.
     *
     * @param shares the component shares making up the split secret.
     * @param coefficients the coefficients associated with the shares.
     * @param witnesses the witnesses associated with the shares.
     * @param commitments the commitments associated with the shares.
     */
    public ECCommittedSplitSecret(BigInteger[] shares, BigInteger[] coefficients, BigInteger[] witnesses, ECPoint[] commitments)
    {
        super(shares, coefficients);

        this.commitments = commitments;
        this.witnesses = witnesses;
    }

    public ECCommittedSecretShare[] getCommittedShares()
    {
        BigInteger[] shares = this.getShares();
        ECCommittedSecretShare[] committedSecretShares = new ECCommittedSecretShare[shares.length];

        for (int i = 0; i != committedSecretShares.length; i++)
        {
            committedSecretShares[i] = new ECCommittedSecretShare(shares[i], witnesses[i], commitments);
        }

        return committedSecretShares;
    }
}
