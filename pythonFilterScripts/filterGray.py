import cv2

# read the image
img = cv2.imread('C:/Images/monaLisa.jpg')

#convert img to grey
img_grey = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

#save result image
cv2.imwrite('C:/Images/filterGrayImage.jpg', img_grey)