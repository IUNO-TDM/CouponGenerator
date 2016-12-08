import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;


/**
 * Copyright 2016 TRUMPF Werkzeugmaschinen GmbH + Co. KG
 * Created by Hans-Peter Bock on 07.12.2016.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class CouponWallet {
    final NetworkParameters params = TestNet3Params.get();

    private Wallet feedWallet;
    private Wallet couponWallet;
    private BlockChain blockChain;
    private PeerGroup peerGroup;

    private static final String FEEDSUFFIX = ".feed";
    private static final String COUPONSUFFIX = ".coupon";
    private static final String WALLETSUFFIX = ".wallet";

    // todo enable class to create wallet systems from optional seed
    public CouponWallet(String couponName, boolean createWallets) throws Exception {
        // construtcor
        String homeDir = System.getProperty("user.home");
        File chainFile = new File(homeDir, couponName + ".spvchain");
        File feedWalletFile = new File(homeDir, couponName + FEEDSUFFIX + WALLETSUFFIX);
        File couponWalletFile = new File(homeDir, couponName + COUPONSUFFIX + WALLETSUFFIX);

        // create new wallet system
        if (createWallets) {
            if (feedWalletFile.exists() || couponWalletFile.exists()) {
                System.out.println("!!! At least one wallet file already exists. No new wallet is created.");
                throw new IllegalStateException("At least one wallet file already exists. No new wallet is created.");
            }
            feedWallet = new Wallet(params);
            couponWallet = new Wallet(params);

            // eventually remove blockchainfile
            if (chainFile.exists()) {
                chainFile.delete();
            }

        } else { // load existing wallet system
            feedWallet = Wallet.loadFromFile(feedWalletFile);
            couponWallet = Wallet.loadFromFile(couponWalletFile);
        }

        // auto save wallets at least every five seconds
        feedWallet.autosaveToFile(feedWalletFile, 5, TimeUnit.SECONDS, null);
        couponWallet.autosaveToFile(couponWalletFile, 5, TimeUnit.SECONDS, null);

        // initialize blockchain file
        List<Wallet> wallets = asList(feedWallet, couponWallet);
        blockChain = new BlockChain(params, wallets, new SPVBlockStore(params, chainFile));

        // initialize peer groupe
        peerGroup = new PeerGroup(params, blockChain);
        peerGroup.addWallet(feedWallet);
        peerGroup.addWallet(couponWallet);
    }

    public void startWalletSystem() {
        peerGroup.start();
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        peerGroup.startBlockChainDownload(new DownloadProgressTracker());
    }

    public void downloadBlockChain() {
        peerGroup.downloadBlockChain();
    }

    public void stopWalletSystem() {
        peerGroup.stop();
    }
}
