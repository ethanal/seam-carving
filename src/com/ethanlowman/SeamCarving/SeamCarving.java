package com.ethanlowman.SeamCarving;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class SeamCarving {
	static JLabel label;
	static JPanel panel;
	static boolean rightClickMode = true;
	static BufferedImage originalImage, currentImage;

	static BufferedImage copyImage(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	static void showImage(BufferedImage img) {
		currentImage = img;
		label.setIcon(new ImageIcon(img));
		panel.revalidate();
	}
	
	static int colorDistance(int c1, int c2) {
		int r = ((c1 >> 16) & 0xFF) - ((c2 >> 16) & 0xFF);
		int g = ((c1 >> 8) & 0xFF) - ((c2 >> 8) & 0xFF);
		int b = (c1 & 0xFF) - (c2 & 0xFF);
		return r*r + g*g + b*b;
	}
	
	static int[][] totalGradient(BufferedImage img) {
		int[][] G = new int[img.getWidth()][img.getHeight()];
		int left, right, up, down;
		
		for (int x = 0; x < img.getWidth(); ++x) {
			for (int y = 0; y < img.getHeight(); ++y) {
				left = img.getRGB(x == 0 ? x : x-1, y);
				right = img.getRGB(x == img.getWidth() - 1 ? x : x+1, y);
				up = img.getRGB(x, y == 0 ? y : y-1);
				down = img.getRGB(x, y == img.getHeight() - 1 ? y : y+1);
				G[x][y] = colorDistance(left, right) + colorDistance(up, down);
			}
		}
		
		return G;
	}
	
	static int[][] verticalDP(int[][] G) {
		int width = G.length;
		int height = G[0].length;
		int[][] dp = new int[width][height];
		
		for (int x = 0; x < width; ++x) {
			dp[x][0] = G[x][0];
		}
		
		for (int y = 1; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				if (x == 0)
					dp[x][y] = G[x][y] + Math.min(dp[x][y-1], dp[x+1][y-1]);
				else if (x == width - 1)
					dp[x][y] = G[x][y] + Math.min(dp[x-1][y-1], dp[x][y-1]);
				else
					dp[x][y] = G[x][y] + Math.min(Math.min(dp[x-1][y-1], dp[x][y-1]),
							                      dp[x+1][y-1]);
			}
		}
		
		return dp;
	}
	
	static int[][] horizontalDP(int[][] G) {
		int width = G.length;
		int height = G[0].length;
		int[][] dp = new int[width][height];
		
		for (int x = 0; x < width; ++x) {
			dp[x][0] = G[x][0];
		}
		
		for (int x = 1; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				if (y == 0)
					dp[x][y] = G[x][y] + Math.min(dp[x-1][y], dp[x-1][y+1]);
				else if (y == height - 1)
					dp[x][y] = G[x][y] + Math.min(dp[x-1][y-1], dp[x-1][y]);
				else
					dp[x][y] = G[x][y] + Math.min(Math.min(dp[x-1][y-1], dp[x-1][y]),
							                      dp[x-1][y+1]);
			}
		}
		
		return dp;
	}
	
	static BufferedImage removeVerticalSeam(BufferedImage img, int k) {
		int[][] G = totalGradient(img);
		int[][] dp = verticalDP(G);
		
		BufferedImage resized = new BufferedImage(img.getWidth() - k,
				                                  img.getHeight(),
				                                  BufferedImage.TYPE_INT_RGB);
		int startCol = 0;
		for (int i = 0; i < img.getWidth(); ++i) {
			if (dp[startCol][img.getHeight() - 1] > dp[i][img.getHeight() - 1])
				startCol = i;
		}
		
		for (int y = resized.getHeight() - 1; y >= 0; y--) {
			for (int x = 0; x < resized.getWidth(); ++x) {
				resized.setRGB(x, y, img.getRGB(x >= startCol ? x+k : x, y));
			}
			
			if (y > 0) {
				int center = dp[startCol][y-1];
				if (startCol > 0 && dp[startCol-1][y-1] <= center)
					startCol -= 1;
				else if (startCol < img.getWidth() - 1 && dp[startCol+1][y-1] <= center)
					startCol += 1;
				
			}
		}
		
		return resized;
		
	}
	
	static BufferedImage removeHorizontalSeam(BufferedImage img, int k) {
		int[][] G = totalGradient(img);
		int[][] dp = horizontalDP(G);
		
		BufferedImage resized = new BufferedImage(img.getWidth(),
				                                  img.getHeight() - k,
				                                  BufferedImage.TYPE_INT_RGB);
		int startRow = 0;
		for (int i = 0; i < img.getHeight(); ++i) {
			if (dp[img.getWidth() - 1][startRow] > dp[img.getWidth() - 1][i])
				startRow = i;
		}
		
		for (int x = resized.getWidth() - 1; x >= 0; x--) {
			for (int y = 0; y < resized.getHeight(); ++y) {
				resized.setRGB(x, y, img.getRGB(x, y >= startRow ? y+k : y));
			}
			
			if (x > 0) {
				int center = dp[x-1][startRow];
				if (startRow > 0 && dp[x-1][startRow-1] <= center)
					startRow -= 1;
				else if (startRow < img.getHeight() - 1 && dp[x-1][startRow+1] <= center)
					startRow += 1;
				
			}
		}
		
		return resized;
		
	}
	
	
	static BufferedImage resize(BufferedImage img, int newWidth, int newHeight) {
		int dr = img.getHeight() - newHeight;
		int dc = img.getWidth() - newWidth;
		

		int k = 4;
		for(int i = dc; i > 0;) {
			if (i > k) {
				img = removeVerticalSeam(img, k);
				i -= k;
			} else {
				img = removeVerticalSeam(img, 1);
				i--;
			}
		}
		
		for(int i = dr; i > 0;) {
			if (i > k) {
				img = removeHorizontalSeam(img, k);
				i -= k;
			} else {
				img = removeHorizontalSeam(img, 1);
				i--;
			}
		}
		
		
		return img;
	}

	public static void main(String[] args) {
		String filename = "images/Yosemite_small.jpg";
		if (args.length > 0) {
			filename = "images/" + args[0];
		}
		
		try {
			originalImage = ImageIO.read(new File(filename));
			currentImage = copyImage(originalImage);
		} catch (IOException e) {
			System.out.println("Error reading file.");
		}
		
		JFrame frame = new JFrame();
		frame.setSize(originalImage.getWidth(), originalImage.getHeight()+22);
		frame.setTitle(filename.substring(filename.lastIndexOf("/") + 1));
		frame.setResizable(false);
		frame.setFocusable(true);
		frame.getContentPane().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					// Alternate between gradient and original image
					if (rightClickMode) {
						currentImage = copyImage(originalImage);
						int[][] G = totalGradient(originalImage);
						for (int x = 0; x < currentImage.getWidth(); ++x) {
							for (int y = 0; y < currentImage.getHeight(); ++y) {
								int v = Math.min((int)Math.sqrt(G[x][y]), 255);
								currentImage.setRGB(x, y, new Color(v, v, v).getRGB());
							}
						}
						showImage(currentImage);
					} else {
						showImage(originalImage);
					}
					rightClickMode = !rightClickMode;
				} else {
					Point point = e.getPoint();
					showImage(resize(currentImage,
									 Math.min(point.x, currentImage.getWidth()),
									 Math.min(point.y, currentImage.getHeight())));
					
					rightClickMode = false;
				}
			}
		});
		
		panel = new JPanel(new BorderLayout());		
		label = new JLabel();
		label.setIcon(new ImageIcon(originalImage));
		panel.add(label, BorderLayout.PAGE_START);
		frame.add(panel);
		
		frame.setVisible(true);
	}
}
