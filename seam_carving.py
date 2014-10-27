#!/usr/bin/env python3

from PIL import Image, ImageTk
import tkinter
import numpy as np
from scipy import misc, signal, ndimage
import sys

INF = float("infinity")


def show_image(I):
    Image.fromarray(np.uint8(I)).show()


def total_gradient(I, seam=None):
    # TODO: only recompute gradient for cells adjacent to removed seam
    kernel_h = np.array([[1, 0, -1]])
    r_h = signal.convolve2d(I[:, :, 0], kernel_h, mode="same", boundary="symm")
    g_h = signal.convolve2d(I[:, :, 1], kernel_h, mode="same", boundary="symm")
    b_h = signal.convolve2d(I[:, :, 2], kernel_h, mode="same", boundary="symm")

    kernel_v = np.array([[1], [0], [-1]])
    r_v = signal.convolve2d(I[:, :, 0], kernel_v, mode="same", boundary="symm")
    g_v = signal.convolve2d(I[:, :, 1], kernel_v, mode="same", boundary="symm")
    b_v = signal.convolve2d(I[:, :, 2], kernel_v, mode="same", boundary="symm")

    return (np.square(r_h) + np.square(g_h) + np.square(b_h) +
            np.square(r_v) + np.square(g_v) + np.square(b_v))


def min_neighbor_index(M, i, j):
    rows, cols = M.shape
    c = M[i-1, j]
    if j > 0 and M[i-1, j-1] < c:
        return -1
    elif j < cols - 1 and M[i-1, j+1] < c:
        return 1
    return 0


def calc_dp(G):
    rows, cols = G.shape
    a = np.copy(G)

    kernel_l = np.array([0, 0, 1])
    kernel_c = np.array([0, 1, 0])
    kernel_r = np.array([1, 0, 0])

    for i in range(rows):
        lefts = ndimage.filters.convolve1d(a[i-1], kernel_l)
        centers = ndimage.filters.convolve1d(a[i-1], kernel_c)
        rights = ndimage.filters.convolve1d(a[i-1], kernel_r)
        a[i] += np.minimum(np.minimum(lefts, centers), rights)
    return a


def find_seam(dp, start_col):
    rows, cols = dp.shape
    seam = np.zeros((rows,), dtype=np.uint32)
    j = seam[-1] = start_col

    for i in range(rows - 2, -1, -1):
        dc = min_neighbor_index(dp, i + 1, j)
        j += dc
        seam[i] = j

    return seam


def find_best_seam(dp):
    start_col = np.argmin(dp[-1])
    return find_seam(dp, start_col)


def remove_seam(M, seam):
    rows, cols = M.shape[:2]
    return np.array([M[i, :][np.arange(cols) != seam[i]] for i in range(rows)])


def resize(I, new_width, new_height):
    rows, cols = I.shape[:2]
    dr = rows - new_height
    dc = cols - new_width

    for i in range(dc):
        G = total_gradient(I)
        dp_v = calc_dp(G)

        seam = find_best_seam(dp_v)
        I = remove_seam(I, seam)

    I = np.swapaxes(I, 0, 1)
    for i in range(dr):
        G = total_gradient(I)
        dp_h = calc_dp(G)

        seam = find_best_seam(dp_h)
        I = remove_seam(I, seam)

    return np.swapaxes(I, 0, 1)


def add_image_to_canvas(I, canvas):
    height, width = I.shape[:2]
    canvas.img_tk = ImageTk.PhotoImage(Image.fromarray(np.uint8(I)))
    canvas.create_image(width // 2, height // 2, image=canvas.img_tk)


def main():
    image_name = sys.argv[1]
    image_path = "images/" + image_name

    I = misc.imread(image_path)
    height, width = I.shape[:2]

    root = tkinter.Tk()
    root.title("Seam Carving - " + image_name)
    root.resizable(width=0, height=0)
    canvas = tkinter.Canvas(root,
                            width=width,
                            height=height,
                            highlightthickness=0)
    canvas.pack()

    add_image_to_canvas(total_gradient(I), canvas)
    print(total_gradient(I))
    canvas.np_img = I

    def click(event):
        max_y, max_x = canvas.np_img.shape[:2]
        if 0 < event.x < max_x and 0 < event.y < max_y:
            canvas.delete("all")
            canvas.np_img = resize(canvas.np_img, event.x, event.y)
            add_image_to_canvas(canvas.np_img, canvas)

    root.bind("<Button-1>", click)
    root.bind("<q>", quit)
    root.mainloop()


if __name__ == "__main__":
    main()
