package iuno.tdm.coupongenerator;

/**
 * Copyright 2017 TRUMPF Werkzeugmaschinen GmbH + Co. KG
 * Created by Hans-Peter Bock on 11.01.2017.
 * Derived from bitcoin-wallet by Andreas Schildbach
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.io.BaseEncoding;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletTransaction;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * This class expects a private key, e.g. from a paper wallet, and shall
 * send all bitcoins available for this private key to a provided bitcoin
 * address.
 */
public class CouponRedemption {
    public static void main(String args[]) {
        getTx();
    }

    static public void getTx() {

        // https://groups.google.com/forum/#!topic/bitcoinj/U-fYsNlyFeY
        // https://groups.google.com/forum/#!searchin/bitcoinj/paper$20wallet|sort:relevance/bitcoinj/evp3jhta_as/S1yTWETyILAJ
        // code derived from https://github.com/bitcoin-wallet/bitcoin-wallet/blob/master/wallet/src/de/schildbach/wallet/ui/send/RequestWalletBalanceTask.java

        // PUB:  mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc

        // https://test-insight.bitpay.com/address/mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc
        // https://testnet.blockexplorer.com/api/addr/%20mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc/utxo
        // [{"address":"mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc","txid":"002a9029529d03f357144f5e43de31248d0ac95d3c2fa682d483b58799666e70","vout":0,"scriptPubKey":"76a9143bc74fcc5fdff2df5c9cd0ef10998680a31995b588ac","amount":0.999,"satoshis":99900000,"confirmations":0,"ts":1484140292}]

        NetworkParameters param = TestNet3Params.get();
        org.bitcoinj.core.Context.propagate(new Context(param));

        final Sha256Hash utxoHash = Sha256Hash.wrap("002a9029529d03f357144f5e43de31248d0ac95d3c2fa682d483b58799666e70"); // txid
        final int utxoIndex = 0; // vout
        final byte[] utxoScriptBytes = BaseEncoding.base16().lowerCase().decode("76a9143bc74fcc5fdff2df5c9cd0ef10998680a31995b588ac");
        final Coin utxoValue = Coin.valueOf(99900000); // satoshis

        Transaction tx = new FakeTransaction(param, utxoHash);
        tx.getConfidence().setConfidenceType(TransactionConfidence.ConfidenceType.BUILDING);

        final TransactionOutput output = new TransactionOutput(param, tx, utxoValue, utxoScriptBytes);


        if (tx.getOutputs().size() > utxoIndex) {
            // Work around not being able to replace outputs on transactions
            final List<TransactionOutput> outputs = new ArrayList<TransactionOutput>(
                    tx.getOutputs());
            final TransactionOutput dummy = outputs.set(utxoIndex, output);
            checkState(dummy.getValue().equals(Coin.NEGATIVE_SATOSHI),
                    "Index %s must be dummy output", utxoIndex);
            // Remove and re-add all outputs
            tx.clearOutputs();
            for (final TransactionOutput o : outputs)
                tx.addOutput(o);
        } else {
            // Fill with dummies as needed
            while (tx.getOutputs().size() < utxoIndex)
                tx.addOutput(new TransactionOutput(param, tx,
                        Coin.NEGATIVE_SATOSHI, new byte[]{}));

            // Add the real output
            tx.addOutput(output);
        }

        ECKey key = DumpedPrivateKey.fromBase58(param, "xxxxxxxx").getKey();
        final KeyChainGroup group = new KeyChainGroup(param);
        group.importKeys(key);
        Wallet walletToSweep = new Wallet(param, group);

        walletToSweep.clearTransactions(0);
        walletToSweep.addWalletTransaction(new WalletTransaction(WalletTransaction.Pool.UNSPENT, tx));

        SendRequest sr = SendRequest.emptyWallet(key.toAddress(param));
        try {
            final Transaction transaction;
            transaction = walletToSweep.sendCoinsOffline(sr);
            System.out.println(BaseEncoding.base16().lowerCase().encode(transaction.bitcoinSerialize()));
        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
        }

    }


    private static class FakeTransaction extends Transaction {
        private final Sha256Hash hash;

        public FakeTransaction(final NetworkParameters params, final Sha256Hash hash) {
            super(params);
            this.hash = hash;
        }

        @Override
        public Sha256Hash getHash() {
            return hash;
        }
    }
}
