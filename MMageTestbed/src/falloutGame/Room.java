package falloutGame;

public class Room
{
	public static enum Direction {
		NORTH(0), SOUTH(1), EAST(2), WEST(3);
		
		public final int index;
		
		private Direction(int index) {
			this.index = index;
		}
	}
	
	// same as: new Room[]{null, null, null, null};
	private Room[] adjacent = new Room[4];
	
	public Room()
	{
		setAdjacent(Direction.NORTH, this);
	}
	
	public void setAdjacent(Direction dir, Room room) {
		this.adjacent[dir.index] = room;
		
	}
	
	public Room move(Direction dir) {
		Room next = this.adjacent[dir.index];
		// If there's an adjacent room in that direction...
		if (next != null)
			// ...return that room.
			return next;
		// Otherwise, they can't move in that direction, so return this room.
		return this;
	}
}

