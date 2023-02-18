package com.minecrafttas.discombobulator.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapPair {
	
	public static <L, R>L getLeft(Map<L, R> map){
		Set<L> leftSet = map.keySet();
		for(L left : leftSet) {
			return left;
		}
		return null;
	}
	
	public static <L, R>R getRight(Map<L, R> map){
		Collection<R> rightCollection = map.values();
		for(R right : rightCollection) {
			return right;
		}
		return null;
	}
}
