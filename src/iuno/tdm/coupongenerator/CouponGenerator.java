package iuno.tdm.coupongenerator;

import org.bitcoinj.core.Coin;

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

        if ("init".equals(command)) createWallets = true;
        if ("reset".equals(command)) resetWallets = true;

        CouponWallet couponWallet = new CouponWallet(name, createWallets, resetWallets);

        couponWallet.startWalletSystem();

        if ("status".equals(command)) {
            couponWallet.downloadBlockChain();
            couponWallet.showStatus();

        } else if (("generate".equals(command)) && (args.length == 4)) {
            int number = Integer.parseUnsignedInt(args[2]);
            Coin value = Coin.parseCoin(args[3]);
            System.out.printf("number: %d - value: %s", number, value.toFriendlyString());
            couponWallet.generateCoupons(number, value);

        } else if ("sweep".equals(command)) {
            couponWallet.sweepCoupons();
        }
        couponWallet.stopWalletSystem();
    }
}
