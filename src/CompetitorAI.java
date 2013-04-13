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
import bonzai.api.Item;
import bonzai.api.Item.Type;
import bonzai.api.list.DuckList;


public class CompetitorAI implements AI {

	//Codes for role assignment
	private final int R_DUCK_FETCH = 0;
	private final int R_RECON = 1;
	private final int R_GRIEF = 2;
	private final int R_PATHS = 3;
	boolean boughtBucket = false;

	private HashMap<Integer, Integer> roles = new HashMap<Integer, Integer>();
	private HashMap<Position, Boolean> duckSpokenFor = new HashMap<Position, Boolean>();
	
	Position homePosition = new Position(-1,-1);
	
	@Override
	public Collection<FarmhandAction> turn(GameState state) {
		
		//Set the home base position
		if (homePosition.equals(new Position(-1, -1)))
			homePosition = state.getMyBase().getPosition();
		
		ArrayList<FarmhandAction> actions = new ArrayList<FarmhandAction>();

		//Number of workers to assign to each role
		int totalWorkers = state.getMyFarmhands().size();
		
		int grief = totalWorkers/2;
		int duckFetch = totalWorkers - grief;

		//role assignment
		for (int index = 0; index < totalWorkers; index++) {
			if (duckFetch > 0) {
				roles.put(index, R_DUCK_FETCH);
				//System.out.println("Adding Fetcher " + duckFetch);
				duckFetch--;
			}
			else if (grief > 0) {
				//System.out.println("Adding grief " + grief);
				roles.put(index, R_GRIEF);
				grief--;
			}
		}

		/* Print out what each farmhand is going to do for this turn
		for (Integer i : roles.keySet()) {
			System.out.println("Farmhand " + i + " is doing " + roles.get(i));
		}
		 */

		int index = 0;
		for (Farmhand farmhand : state.getMyFarmhands()) {
			if (roles.get(index) == R_DUCK_FETCH)
				actions.add(duckFetch(state, farmhand));
			else if (roles.get(index) == R_GRIEF && !farmhand.isStumbled())
				actions.add(grief(state, farmhand));
			else
				actions.add(noJob(state, farmhand));
			index++;
		}

		return actions;
	}

	private FarmhandAction noJob(GameState state, Farmhand farmhand) {
		return farmhand.shout("I'm lazy and have no job");
	}

	private FarmhandAction grief(GameState state, Farmhand farmhand) {
		// Get the closest visible duck owned by other team
		DuckList ducks = state.getDucks().getNotHeld();
		ducks.removeAll(state.getMyDucks());
		Duck closestDuck = ducks.getClosestTo(farmhand);
		if (closestDuck != null) {

			int dx = farmhand.getX() - closestDuck.getX();
			int dy = farmhand.getY() - closestDuck.getY();

			// If not adjacent to the duck
			if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {

				int newX = farmhand.getX()
						+ (int) Math.signum(closestDuck.getX()
								- farmhand.getX());
				int newY = farmhand.getY()
						+ (int) Math.signum(closestDuck.getY()
								- farmhand.getY());
				// Move closer to the duck, if the tile is crossable
				if (state.isTileEmpty(newX, newY)
						&& state.getTile(newX, newY).canFarmhandCross()) {
					return farmhand.move(newX, newY);
				}
			} 
		}
		return farmhand.shout("I'm griefing");
	}

	private FarmhandAction duckFetch(GameState state, Farmhand farmhand) {
		Entity item = farmhand.getHeldObject();
		DuckList currentDucks = state.getMyDucks().getNotHeld();
		Position farmhandPosition = farmhand.getPosition();
		
		ArrayList<Duck> possibleDucks = getCurrentDucksByDistance(state, farmhandPosition);
		Duck closest = null;
		
		for (Duck d : possibleDucks) {
			//Check the hashmap to see if this duck is spoken for
			if (!duckSpokenFor.get(d.getPosition())) {
				closest = d;
				break;
			}
		}
		
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
	
	private ArrayList<Duck> getCurrentDucksByDistance(GameState state, Position farmhandPosition) {
		ArrayList<Duck> ducks = new ArrayList<Duck>();
		
		for (Duck d : state.getMyDucks().getNotHeld()) {
			for (int i = 0; i < ducks.size(); i++) {
				if (distance(d.getPosition(), farmhandPosition) < 
					distance(ducks.get(i).getPosition(), farmhandPosition)) {
					ducks.add(i, d);
				}
			}
		}
		System.out.println(ducks);
		return ducks;
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
