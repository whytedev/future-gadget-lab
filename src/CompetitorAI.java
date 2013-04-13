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
	private boolean boughtBucket = false;
	private ArrayList<Duck> enemyDucks =  new ArrayList<Duck>();

	private HashMap<Integer, Integer> roles = new HashMap<Integer, Integer>();
	private ArrayList<Duck> ourDucks = new ArrayList<Duck>();

	Position homePosition = new Position(-1,-1);

	@Override
	public Collection<FarmhandAction> turn(GameState state) {
		enemyDucks.clear();
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

		//Clear out the HashMap
		ourDucks.clear();

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
		ducks.removeAll(enemyDucks);
		Duck closestDuck = ducks.getClosestTo(farmhand);
		if (closestDuck != null || !enemyDucks.isEmpty()) {
			if(closestDuck == null){
				closestDuck = enemyDucks.get(0);
			}
			else{
				enemyDucks.add(closestDuck);
			}
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
		Position farmhandPosition = farmhand.getPosition();

		if (item instanceof Duck) {
			//System.out.println("Holding duck");
			if (farmhandPosition.equals(homePosition))
				farmhand.dropItem(homePosition);
			return farmhand.move(shortestPath(state, farmhandPosition, homePosition));
		}

		DuckList possibleDucks = state.getMyDucks().getNotHeld();
		possibleDucks.removeAll(ourDucks);

		Duck closest = possibleDucks.getClosestTo(farmhandPosition);

		ourDucks.add(closest);

		//need to find the closest duck and go towards it
		//System.out.println("Going twoards duck: " + closest.getPosition());
		if (closest == null)
			return farmhand.shout("I'm waiting for chickens");

		if (distance(farmhandPosition, closest.getPosition()) < 1.5) {
			//System.out.println("Returning duck to base");
			return farmhand.pickUp(closest);
		}

		return farmhand.move(shortestPath(state, farmhandPosition, closest.getPosition()));
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
		//System.out.println(closest);
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
