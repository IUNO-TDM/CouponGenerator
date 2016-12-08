# Coupon Generator
Bitcoin based coupon generator used for the IUNO technology data marketplace demonstrator.

## Commands
* **initialize** this command will initialize a new wallet system for the generation of coupons
* **generate** this command will generate a specified amount of coupons of a specified value
* **sweep** this command will sweep all expired coupons
* **status** this command will show how much value is left in the feed wallet and the coupons

## Parameters
* **expiration** the expiration date of coupons generated by the wallet
* **name** name of the coupon system
* **seed** optional seed to create HD wallet system
* **number** number of coupons to generate
* **value** value that each coupon shall get