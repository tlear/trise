package com.tlear.trise.environment;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.tlear.trise.graph.Graph;
import com.tlear.trise.objects.Agent;
import com.tlear.trise.objects.StaticGoal;
import com.tlear.trise.objects.StaticObstacle;
import com.tlear.trise.utils.Triple;
import com.tlear.trise.utils.Tuple;

public class Environment {
	public final int maxX;
	public final int maxY;

	public LinkedList<Agent> agents;
	public LinkedList<StaticObstacle> obstacles;
	public LinkedList<StaticGoal> goals;
	
	@Override
	public String toString() {
		return "Environment [maxX=" + maxX + ", maxY=" + maxY + ", agents="
				+ agents + ", obstacles=" + obstacles + ", goals=" + goals
				+ "]";
	}

	public Environment() {
		agents = new LinkedList<Agent>();
		obstacles = new LinkedList<StaticObstacle>();
		goals = new LinkedList<StaticGoal>();
		
		maxX = 600;
		maxY = 480;
	}
	
	public Environment(int maxX, int maxY) {
		agents = new LinkedList<Agent>();
		obstacles = new LinkedList<StaticObstacle>();
		goals = new LinkedList<StaticGoal>();
		
		this.maxX = maxX;
		this.maxY = maxY;
	}
	
	public Environment(Environment that) {
		this.maxX = that.maxX;
		this.maxY = that.maxY;
		
		this.agents = new LinkedList<Agent>(that.agents);
		this.obstacles = new LinkedList<StaticObstacle>(that.obstacles);
		this.goals = new LinkedList<StaticGoal>(that.goals);
	}
	
	public boolean placeAgent(Agent a) {
		/*
		 * We'll want to do some checking that we *can* place this agent
		 */
		agents.add(a);
		return true;
	}
	
	/**
	 * Generates the next keyframe
	 * @param timeMap
	 */
	public Triple<Integer, Integer, Graph<Vector2>> getNextKeyframe(Map<Integer, Integer> timeMap) {
		
//		System.out.println("GETTING NEXT KEYFRAME");
		/*
		 * we'll probably want to check that we are actually at the latest keyframe
		 */
		
		Triple<Environment, Tuple<Integer, Integer>, Graph<Vector2>> nextKeyframe = agents.getFirst().process(this, timeMap);
//		System.out.println("NEXT KEYFRAME: " + nextKeyframe);
		Tuple<Integer, Integer> mapEntry = nextKeyframe.snd;
		
		return new Triple<Integer, Integer, Graph<Vector2>>(mapEntry.fst, mapEntry.snd, nextKeyframe.thd);
	}
	
	public void update(Map<Integer, Integer> timeMap, int prevKeyframe, int time, int nextKeyframe) {
		
		/* prevKeyframe = k-1
		 * nextKeyframe = k
		 *
		 * PRE: k-1 < k
		 * 		k-1 > 0
		 * 		k <= lastKeyframe
		 * 		timeMap[k-1] <= t <= timeMap[k]
		 * 		timeMap.length > 0
		 */
		
//		System.out.println(time);
		
		int lastKeyframe = findLastKeyframe(timeMap);
		
		if (lastKeyframe < 0) {
			throw new RuntimeException("Invalid time map " + timeMap.toString());
		}
		if (prevKeyframe >= nextKeyframe) {
			throw new RuntimeException("Previous keyframe must be less than next keyframe");
		}
		if (prevKeyframe < 0) {
			throw new RuntimeException("Trying to interpolate from a negative keyframe: " + prevKeyframe);
		}
		if (nextKeyframe > lastKeyframe) {
			throw new RuntimeException("Trying to interpolate to a non-existent keyframe: " + nextKeyframe + ".  Most recent keyframe is " + lastKeyframe);
		}
		if (timeMap.get(prevKeyframe) > time || timeMap.get(nextKeyframe) < time) {
			throw new RuntimeException("Trying to interpolate for a time outside the keyframe bounds: " + time + ".  Bounds are " + timeMap.get(prevKeyframe) + " and " + timeMap.get(nextKeyframe));
		}
		
		/*
		 * Loop through agents
		 */
		
		for (Agent a : agents) {
			a.update(timeMap, prevKeyframe, time, nextKeyframe);
//			System.out.println(a);
		}
	}
	
	public void draw(ShapeRenderer sr) {
		for (Agent a : agents) {
			a.draw(sr);
		}
	}
	
	private int findLastKeyframe(Map<Integer, Integer> timeMap) {
		// Finds the last keyframe as the largest value in the keys of timeMap
		return timeMap.keySet().stream().max((x, y) -> x - y).orElse(-1);
	}
}
