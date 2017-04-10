# Coupon Generator
Bitcoin based coupon generator used for the IUNO technology data marketplace demonstrator.

``java -jar CouponGenerator.jar <command> <name> [parameters]``

## Commands
* **initialize** this command will initialize a new wallet system for the generation of coupons
* **status** this command will show how much value is left in the feed wallet and the coupons
* **generate** this command will generate a specified amount of coupons of a specified value
* **sweep** this command will sweep all expired coupons
* **reset** reset wallet and blockchain

## Parameters
* --expiration-- the expiration date of coupons generated by the wallet
* --seed-- optional seed to create HD wallet system
* **number** number of coupons to generate
* **value** value that each coupon shall get

# Examples

## create and initialize coupon system
``java -jar CouponGenerator.jar init myCoupons``

## show status of coupon system
``java -jar CouponGenerator.jar status myCoupons``

## generate coupons
``java -jar CouponGenerator.jar generate myCoupons <amount> <value>``
``java -jar CouponGenerator.jar generate myCoupons 100 0.005``

## sweep all coupons to feed wallet
``java -jar CouponGenerator.jar sweep myCoupons``

## reset wallets to redownload blockchain
``java -jar CouponGenerator.jar reset myCoupons``

