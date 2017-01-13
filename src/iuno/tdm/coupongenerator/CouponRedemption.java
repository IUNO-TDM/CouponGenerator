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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * This class expects a private key, e.g. from a paper wallet, and shall
 * send all bitcoins available for this private key to a provided bitcoin
 * address.
 */

// cUNuqzcxHMqPVHcTKmWTy1PYmVMB5y1TaNpLUTFFqTYPVGE82sWs: mgDFwLxmQ7PS8rMZQXFLB2p43VWt7xBHTZ

public class CouponRedemption {
    static private NetworkParameters param = TestNet3Params.get();
    private ECKey ecKey;

    public CouponRedemption(String wif) {
        org.bitcoinj.core.Context.propagate(new Context(param));
        ecKey = DumpedPrivateKey.fromBase58(param, wif).getKey();
    }

    public static void main(String args[]) {
        CouponRedemption coupon = new CouponRedemption("cUNuqzcxHMqPVHcTKmWTy1PYmVMB5y1TaNpLUTFFqTYPVGE82sWs");

        final String response = coupon.getUtxoString();
        System.out.println(response);

        final Map<Sha256Hash, Transaction> transactions = coupon.getTransactionsForUtxoString(response);
        String tx = coupon.sweepUtxo(transactions);
        System.out.println(tx);
    }

    static public void getTx() {

        // https://groups.google.com/forum/#!topic/bitcoinj/U-fYsNlyFeY
        // https://groups.google.com/forum/#!searchin/bitcoinj/paper$20wallet|sort:relevance/bitcoinj/evp3jhta_as/S1yTWETyILAJ
        // code derived from https://github.com/bitcoin-wallet/bitcoin-wallet/blob/master/wallet/src/de/schildbach/wallet/ui/send/RequestWalletBalanceTask.java

        // PUB:  mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc

        // https://test-insight.bitpay.com/address/mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc
        // https://testnet.blockexplorer.com/api/addr/%20mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc/utxo
        // [{"address":"mky2vnvS8LcHFdwGjTVKa7PkXSy17e5HRc","txid":"002a9029529d03f357144f5e43de31248d0ac95d3c2fa682d483b58799666e70","vout":0,"scriptPubKey":"76a9143bc74fcc5fdff2df5c9cd0ef10998680a31995b588ac","amount":0.999,"satoshis":99900000,"confirmations":0,"ts":1484140292}]

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

        ECKey key = DumpedPrivateKey.fromBase58(param, "cUNuqzcxHMqPVHcTKmWTy1PYmVMB5y1TaNpLUTFFqTYPVGE82sWs").getKey();
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

    private String getUtxoString() {
        URL url;
        String response = "";
        try {
            url = new URL("https://testnet.blockexplorer.com/api/addr/" + getAddress() + "/utxo");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                response += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    private Map<Sha256Hash, Transaction> getTransactionsForUtxoString(String str) {
        final JSONArray json = new JSONArray(str);

        System.out.println("Array length: " + json.length());

        final Map<Sha256Hash, Transaction> transactions = new HashMap<Sha256Hash, Transaction>(
                json.length());

        for (int i = 0; i < json.length(); i++) {
            final JSONObject jsonObject = json.getJSONObject(i);
            final String txId = jsonObject.getString("txid");
            final Sha256Hash utxoHash = Sha256Hash.wrap(txId); // txid
            final int utxoIndex = jsonObject.getInt("vout"); // vout
            final byte[] utxoScriptBytes = BaseEncoding.base16().lowerCase().decode(
                    jsonObject.getString("scriptPubKey"));
            final Coin utxoValue = Coin.valueOf(jsonObject.getLong("satoshis")); // satoshis


            Transaction tx = transactions.get(utxoHash);
            if (tx == null) {
                tx = new FakeTransaction(param, utxoHash);
                tx.getConfidence().setConfidenceType(TransactionConfidence.ConfidenceType.BUILDING);
                transactions.put(utxoHash, tx);
            }

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
        }
        return transactions;
    }

    private String sweepUtxo(Map<Sha256Hash, Transaction> transactions) {
        final KeyChainGroup group = new KeyChainGroup(param);
        String ret = "";
        group.importKeys(ecKey);
        Wallet walletToSweep = new Wallet(param, group);

        walletToSweep.clearTransactions(0);
        for (final Transaction tx : transactions.values())
            walletToSweep.addWalletTransaction(new WalletTransaction(WalletTransaction.Pool.UNSPENT, tx));

        Coin dust = Transaction.MIN_NONDUST_OUTPUT;
        Coin balance = walletToSweep.getBalance();

        if (dust.isLessThan(balance.multiply(2))) {
            SendRequest sr = SendRequest.emptyWallet(Address.fromBase58(param, "mzM2i82Y9e4ZDwQVWqY4HcJbuAHYQdXd7A"));
            sr.feePerKb = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
            try {
                final Transaction transaction;
                transaction = walletToSweep.sendCoinsOffline(sr);
                ret = BaseEncoding.base16().lowerCase().encode(transaction.bitcoinSerialize());
            } catch (InsufficientMoneyException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Too few coins in wallet: " + walletToSweep.getBalance().toFriendlyString());
        }
        return ret;
    }

    private String getAddress() {
        return ecKey.toAddress(TestNet3Params.get()).toBase58();
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
