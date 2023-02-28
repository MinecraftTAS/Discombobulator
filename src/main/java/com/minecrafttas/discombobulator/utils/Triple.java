package com.minecrafttas.discombobulator.utils;

public class Triple<L, M, R> {
	
	private L left;
	
	private M middle;
	
	private R right;

	public Triple(L left, M middle, R right) {
		this.left = left;
		this.middle = middle;
		this.right = right;
	}

	public L left() {
		return left;
	}

	public M middle() {
		return middle;
	}

	public R right() {
		return right;
	}
	
	public static <L, M, R>Triple<L, M, R> of(L left, M middle, R right){
		return new Triple<L, M, R>(left, middle, right);
	}
	
}
