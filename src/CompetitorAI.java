import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import bonzai.api.AI;
import bonzai.api.Duck;
import bonzai.api.Entity;
import bonzai.api.Farmhand;
import bonzai.api.FarmhandAction;
import bonzai.api.GameState;
import bonzai.api.Position;
import bonzai.api.Tile;
import bonzai.api.list.DuckList;


public class CompetitorAI implements AI {
	
	//Codes for role assignment
	private final int R_DUCK_FETCH = 0;
	private final int R_RECON = 1;
	private final int R_GRIEF = 2;
	private final int R_PATHS = 3;
	
	private HashMap<Integer, Integer> roles = new HashMap<Integer, Integer>();
	
	
	@Override
	public Collection<FarmhandAction> turn(GameState state) {
		
		ArrayList<FarmhandAction> actions = new ArrayList<FarmhandAction>();
		
		//Number of workers to assign to each role
		int totalWorkers = state.getMyFarmhands().size();
		int duckFetch = totalWorkers/2;
		int recon = 1;
		int grief = 1;
		int pathbuilding = totalWorkers - duckFetch - recon - grief;
		
		//role assignment
		for (int index = 0; index < totalWorkers; index++) {
			if (duckFetch > 0) {
				roles.put(index, R_DUCK_FETCH);
				duckFetch--;
			}
			else if (recon > 0) {
				roles.put(index,  R_RECON);
				recon--;
			}
			else if (grief > 0) {
				roles.put(index, R_GRIEF);
				grief--;
			}
			else if (pathbuilding > 0) {
				roles.put(index, R_PATHS);
				pathbuilding--;
			}
		}
		
		/* Print out what each farmhand is going to do for this turn */
		for (Integer i : roles.keySet()) {
			System.out.println("Farmhand " + i + " is doing " + roles.get(i));
		}
		
		
		int index = 0;
		for (Farmhand farmhand : state.getMyFarmhands()) {
			if (roles.get(index) == R_DUCK_FETCH)
				actions.add(duckFetch(state, farmhand));
			else if (roles.get(index) == R_RECON)
				actions.add(recon(state, farmhand));
			else if (roles.get(index) == R_GRIEF)
				actions.add(grief(state, farmhand));
			else if (roles.get(index) == R_PATHS)
				actions.add(buildPaths(state, farmhand));
			else
				actions.add(noJob(state, farmhand));
			index++;
		}
		
		return actions;
	}

	private FarmhandAction noJob(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm lazy and have no job");
	}

	private FarmhandAction buildPaths(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm building paths");
	}

	private FarmhandAction grief(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm griefing");
	}

	private FarmhandAction recon(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm reconing");
	}

	private FarmhandAction duckFetch(GameState state, Farmhand farmhand) {
		Entity item = farmhand.getHeldObject();
		DuckList currentDucks = state.getMyDucks().getNotHeld();
		Position farmhandPosition = farmhand.getPosition();
		Position homePosition = state.getMyBase().getPosition();
		Duck closest = currentDucks.getNotHeld().getClosestTo(farmhand);
		
		//Find out if there is a duck in adjacent square
		Duck adjacentDuck = null;
		for (Position p : getAdjacent(state, farmhandPosition)) {
			if (p.equals(closest.getPosition()))
				adjacentDuck = closest;
		}
		
		//if we are holding a duck, we want to make progress back to the base
		//otherwise we want to go get a duck
		if (item instanceof Duck) {
			System.out.println("Holding duck");
			if (farmhandPosition.equals(homePosition))
				farmhand.dropItem(homePosition);
			return farmhand.move(shortestPath(state, farmhandPosition, homePosition));
		}
		else if (adjacentDuck != null) {
			System.out.println("Picking up duck");
			return farmhand.pickUp(adjacentDuck);
		}
		else {
			//need to find the closest duck and go towards it
			System.out.println("Going twoards duck: " + closest.getPosition());
			if (closest != null)
				return farmhand.move(shortestPath(state, farmhandPosition, closest.getPosition()));
			else
				return farmhand.shout("No ducks nearby!");
		}
	}
	
	private Position shortestPath(GameState state,
			Position farmhandPosition, Position destination) {
		
		//Get all of the adjacents, to the current position, whichever one
		//of them is closest to the destination return that one
		
		double distance = -1;
		Position closest = null;
		for (Position p : getAdjacent(state, farmhandPosition)) {
			double currentDistance = distance(p, destination);
			if (distance < 0 || currentDistance < distance) {
				distance = currentDistance;
				closest = p;
			}
		}
		System.out.println(closest);
		return closest;
	}

	/* Works apparently */
	private ArrayList<Position> getAdjacent(GameState state, Position toCheck) {
		ArrayList<Position> adjacentPositions = new ArrayList<Position>();
		ArrayList<Position> possible = new ArrayList<Position>();
		
		//Check all 8 corresponding squares
		possible.add(new Position(toCheck.getX() - 1, toCheck.getY() + 1));
		possible.add(new Position(toCheck.getX(), toCheck.getY() + 1));
		possible.add(new Position(toCheck.getX() + 1, toCheck.getY() + 1));
		possible.add(new Position(toCheck.getX() + 1, toCheck.getY()));
		possible.add(new Position(toCheck.getX() + 1, toCheck.getY() - 1));
		possible.add(new Position(toCheck.getX(), toCheck.getY() - 1));
		possible.add(new Position(toCheck.getX()-1, toCheck.getY() - 1));
		possible.add(new Position(toCheck.getX() - 1, toCheck.getY()));
		
		for (Position p : possible) {
			if (state.getTile(p) != null && validTile(state.getTile(p))) {
				adjacentPositions.add(p);
			}
		}
		/*
		System.out.println(toCheck);
		System.out.println(adjacentPositions);
		*/
		return adjacentPositions;
	}
	
	private boolean validTile(Tile t) {
		return t.canFarmhandCross();
	}

	private double distance(Position p1, Position p2) {
		return Math.sqrt(
				Math.pow((p1.getX() - p2.getX()), 2) + 
				Math.pow((p1.getY() - p2.getY()), 2)
		);
	}
}
