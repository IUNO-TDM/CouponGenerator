package iuno.tdm.coupongenerator;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

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
public class CouponGenerator {

    public static void main(String args[]) throws Exception { // todo main shall not throw exceptions

        if (args.length < 2) {
            System.out.println("!!! Too few arguments.");
            return;
        }

        String command = args[0];
        String name = args[1];

        boolean createWallets = false;
        boolean resetWallets = false;

        if (command.startsWith("init")) createWallets = true;
        if (command.equals("reset")) resetWallets = true;

        CouponWallet couponWallet = new CouponWallet(name, createWallets, resetWallets);

        couponWallet.startWalletSystem();

        if (command.equals("status")) {
            couponWallet.downloadBlockChain();
            couponWallet.showStatus();

        } else if ((command.equals("generate")) && (args.length == 4)) {
            int number = Integer.parseUnsignedInt(args[2]);
            Coin value = Coin.parseCoin(args[3]);
            System.out.printf("number: %d - value: %s\n", number, value.toFriendlyString());
            ArrayList<ECKey> ecKeys = couponWallet.generateCoupons(number, value);

            saveCouponsToFile(value, ecKeys);

        } else if (command.equals("sweep")) {
            couponWallet.sweepCoupons();

        } else {
            System.out.println("!!! command is unknown");
        }

        couponWallet.stopWalletSystem();
    }

    private static void saveCouponsToFile(Coin couponValue, ArrayList<ECKey> ecKeys) throws IOException {
        NetworkParameters params = TestNet3Params.get();;
        String homeDir = System.getProperty("user.home");
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYYMMddHHmmss"));
        File couponsFile = new File(homeDir,dateStr + "_coupons.csv");
        FileOutputStream fs = new FileOutputStream(couponsFile,true);
        PrintStream ps = new PrintStream(fs);

        for(ECKey key: ecKeys){
            ps.println(key.toAddress(params) + "," + key.getPrivateKeyAsWiF(params) + "," + couponValue.toFriendlyString());
        }
        ps.flush();
        fs.flush();
        ps.close();
        fs.close();
    }
}
