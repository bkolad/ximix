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
package org.cryptoworkshop.ximix.demo.ballot;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bouncycastle.crypto.ec.ECElGamalEncryptor;
import org.bouncycastle.crypto.ec.ECPair;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.math.ec.ECPoint;
import org.cryptoworkshop.ximix.client.KeyService;
import org.cryptoworkshop.ximix.client.connection.XimixRegistrar;
import org.cryptoworkshop.ximix.client.connection.XimixRegistrarFactory;
import org.cryptoworkshop.ximix.common.asn1.board.PairSequence;
import org.cryptoworkshop.ximix.common.util.EventNotifier;

/**
 * Generator for ballot files.
 */
public class Main
{
    private static ECPoint generatePoint(ECDomainParameters params, SecureRandom rand)
    {
        return params.getG().multiply(getRandomInteger(params.getN(), rand));
    }

    private static BigInteger getRandomInteger(BigInteger n, SecureRandom rand)
    {
        BigInteger r;
        int maxbits = n.bitLength();
        do
        {
            r = new BigInteger(maxbits, rand);
        }
        while (r.compareTo(n) >= 0);
        return r;
    }

    private static ECPair[][] generateBallots(Random rand, ECElGamalEncryptor encryptor, ECDomainParameters ecParams, SecureRandom pointRandom)
    {
        int numberOfCandidates = 4 + rand.nextInt(10);

        List<ECPoint> candidateNumbers = new ArrayList<>(numberOfCandidates);

        for (int candidateNo = 0; candidateNo != numberOfCandidates; candidateNo++)
        {
            candidateNumbers.add(generatePoint(ecParams, pointRandom));
        }

        int numberOfBallots = 100 + rand.nextInt(200);

        ECPair[][] ballots = new ECPair[numberOfBallots][];

        for (int ballotNo = 0; ballotNo != numberOfBallots; ballotNo++)
        {
            Collections.shuffle(candidateNumbers, rand);

            ECPair[] ballot = new ECPair[numberOfCandidates];

            for (int i = 0; i != ballot.length; i++)
            {
                ballot[i] = encryptor.encrypt(candidateNumbers.get(i));
            }

            ballots[ballotNo] = ballot;
        }

        return ballots;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 2)
        {
            System.err.println("Usage: Generate mixnet.xml number_of_regions");
            System.exit(1);
        }

        XimixRegistrar adminRegistrar = XimixRegistrarFactory.createAdminServiceRegistrar(new File(args[0]), new EventNotifier()
        {
            @Override
            public void notify(Level level, Throwable throwable)
            {
                System.err.print(level + " " + throwable.getMessage());
                throwable.printStackTrace(System.err);
            }

            @Override
            public void notify(Level level, Object detail)
            {
                System.err.println(level + " " + detail.toString());
            }

            @Override
            public void notify(Level level, Object detail, Throwable throwable)
            {
                System.err.println(level + " " + detail.toString());
                throwable.printStackTrace(System.err);
            }
        });

        KeyService keyGenerationService = adminRegistrar.connect(KeyService.class);

        byte[] encPubKey = keyGenerationService.fetchPublicKey("ECENCKEY");

        ECPublicKeyParameters pubKey = (ECPublicKeyParameters)PublicKeyFactory.createKey(encPubKey);

        ECElGamalEncryptor encryptor = new ECElGamalEncryptor();

        encryptor.init(pubKey);

        DecimalFormat fmt = new DecimalFormat("000");

        int count = Integer.parseInt(args[1]);

        for (int i = 0; i != count; i++)
        {
            File f = new File("REGION-" + fmt.format(i));

            ECPair[][] ballots = generateBallots(new Random(i), encryptor, pubKey.getParameters(), new SecureRandom());

            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(f));

            for (int j = 0; j != ballots.length; j++)
            {
                fOut.write(new PairSequence(ballots[j]).getEncoded());
            }

            fOut.close();
        }
    }
}
