from Adafruit_Thermal import *
import qrcode
import csv
import glob
import os


# You need to install pyserial, pillow, pyqrcode

def checkIfUsed(row, usedFile):

    usedReader = csv.reader(usedFile)
    for usedRow in usedReader:
        if row[0] == usedRow[0]:
            return True
    return False

def printCoupon(row):
    printer = Adafruit_Thermal("/dev/tty.usbserial", 19200, timeout=5)
    printer.wake()
    printer.justify('C')
    printer.setSize('L')
    printer.println('IUNO Coupon')
    printer.setSize('S')
    printer.println('Value: ' + row[2])
    qr = qrcode.QRCode(
        version=4,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=9,
        border=5,
    )
    qr.add_data(row[1])
    qr.make()
    img = qr.make_image(fill_color="white", back_color="black")
    print(img.size)
    printer.printImage(img)
    printer.println(row[1])
    printer.feed(2)
    # printer.sleep()
    printer.flush()

filelist = glob.glob('*_coupons.csv')
success = False
for filename in filelist:
    if success:
        break
    with open(filename,'rb') as couponFile:
        filenameonly, file_extension = os.path.splitext(filename)
        usedFilename = filenameonly + '_used.csv'
        if not os.path.exists(usedFilename):
            open(usedFilename,'a').close()

        with open(usedFilename) as usedReadFile:
            couponReader = csv.reader(couponFile)
            for row in couponReader:
                if not checkIfUsed(row, usedReadFile):
                    writtenrow = row
                    printCoupon(row)
                    success = True
                    break

        if writtenrow is not None:
            with open(usedFilename, 'a') as usedWriteFile:
                usedWriter = csv.writer(usedWriteFile)
                usedWriter.writerow(writtenrow)
        else:
            print('all coupons in ' +filename + ' are used' )