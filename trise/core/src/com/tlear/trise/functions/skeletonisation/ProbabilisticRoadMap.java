package com.tlear.trise.functions.skeletonisation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.badlogic.gdx.math.Vector2;
import com.tlear.trise.environment.Environment;
import com.tlear.trise.graph.Node;
import com.tlear.trise.graph.TrackedGraph;
import com.tlear.trise.graph.TrackedUndirectedGraph;
import com.tlear.trise.objects.StaticObstacle;

public class ProbabilisticRoadMap implements S13n {

	/**
	 * The proportion of points to place on the map
	 */
	private int noPoints;
	protected int maxNeighbours;

	public ProbabilisticRoadMap(int noPoints, int maxNeighbours) {
		this.noPoints = noPoints;
		this.maxNeighbours = maxNeighbours;
	}

	@Override
	public TrackedGraph<Vector2> skeletonise(Environment t) {

		Environment env = new Environment(t);
		LinkedList<StaticObstacle> newObs = new LinkedList<StaticObstacle>();

		for (StaticObstacle o : env.obstacles) {
			StaticObstacle p = new StaticObstacle(o);
			p.pos.sub(env.agents.getFirst().width / 2, env.agents.getFirst().height / 2);
			p.width += env.agents.getFirst().width;
			p.height += env.agents.getFirst().height;
			newObs.add(p);
		}

		env.obstacles = new LinkedList<>(newObs);

		Set<Vector2> points = new HashSet<>();

		for (int i = 0; i < noPoints; i++) {
			// We place a point randomly in the map and keep trying until we get
			// a safe one
			boolean placed = false;
			Vector2 newPoint = new Vector2();
			while (!placed) {
				newPoint = new Vector2((int) (Math.random() * env.maxX), (int) (Math.random() * env.maxY));

				// Check if the point is unique
				boolean unique = !points.contains(newPoint);

				boolean placeable = true;
				// Check if point is within any obstacles
				for (StaticObstacle o : env.obstacles) {
					if (o.containsPoint(newPoint)) {
						placeable = false;
						points.remove(newPoint);
						break;
					}
				}

				// Check if point is outside of bounds
				boolean withinBounds = newPoint.x >= 0 && newPoint.x <= env.maxX && newPoint.y >= 0 && newPoint.y <= env.maxY;

				placed = unique && placeable && withinBounds;
			}

			points.add(newPoint);
		}

		// Once we have all the points, we create a graph using those as the
		// nodes
		TrackedGraph<Vector2> roadmap = new TrackedUndirectedGraph<Vector2>(env.agents.getFirst().pos);
		env.goals.stream().forEach(g -> roadmap.addNode(g.pos));
		points.stream().forEach(p -> roadmap.addNode(p));

		/*
		 * We then go through the roadmap and connect each point to its closest
		 * legal neighbours. A legal neighbour is one we don't have to draw a
		 * line through a rectangle to get to.
		 */
		for (int n = 0; n < roadmap.getNodes().size(); n++) {
			LinkedList<Node<Vector2>> nearestNeighbours = new LinkedList<Node<Vector2>>();
			Node<Vector2> node = roadmap.getNodes().get(n);

			// First we generate all legal links
			for (Node<Vector2> m : roadmap.getNodes()) {
				if (!node.equals(m)) {

					boolean legalEdge = true;

					for (StaticObstacle o : env.obstacles) {
						if (o.intersectsLine(node.getValue(), m.getValue())) {
							legalEdge = false;
							break;
						}
					}

					if (legalEdge) {
						nearestNeighbours.add(m);
					}
				}
			}

			// Then we sort and only take the closest ones
			nearestNeighbours.sort((x, y) -> isCloser(x.getValue(), y.getValue(), node.getValue()));
			nearestNeighbours = new LinkedList<>(nearestNeighbours.subList(0, Math.min(maxNeighbours, nearestNeighbours.size())));

			for (Node<Vector2> nearestNeighbour : nearestNeighbours) {
				roadmap.addEdge(node, nearestNeighbour);
			}
		}
		return roadmap;
	}

	private int isCloser(Vector2 p, Vector2 q, Vector2 s) {
		// Returning sp - sq will give us which is further away
		float dist = (p.cpy().sub(s).len() - q.cpy().sub(s).len());
		if (dist == 0.0) {
			return 0;
		}
		if (dist > 0.0) {
			return 1;
		}
		if (dist < 0.0) {
			return -1;
		}
		throw new RuntimeException("ERROR");
	}
	
	public String getName() {
		return "Probabilistic Road Map";
	}
}
