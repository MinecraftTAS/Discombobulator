package com.minecrafttas.discombobulator.utils;

public class Pair<L, R> {
	
	private L left;
	
	private R right;
	
	Pair(L left, R right){
		this.left = left;
		this.right = right;
	}
	
	public L left() {
		return left;
	}
	
	public R right() {
		return right;
	}
	
	public static <L, R>Pair<L, R> of(L left, R right){
		return new Pair<L, R>(left, right);
	}
}
