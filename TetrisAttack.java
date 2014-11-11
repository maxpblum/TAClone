//package TA;

import java.awt.*;
import java.util.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class TetrisAttack
{
  public static void main(String args[]) 
  {
        TetrisAttack game = new TetrisAttack();
        game.run();
  }
  
  //***
  //Display modes allowed:
  //***
  private static final DisplayMode POSSIBLE_MODES[] = 
  {
        new DisplayMode(800, 600, 32, 0),
        //new DisplayMode(800, 600, 24, 0),
        //new DisplayMode(800, 600, 16, 0)
  };
  
  private ScreenManager screen;
  
  private static final long DEMO_TIME = 60000;
  private static final short BLOCK_SIDE = 28;
  private static final short SPACE_SIDE = BLOCK_SIDE + 2;
  private static final short COLUMNS = 6;
  private static final short TOTAL_ROWS = 17;
  private static final short TOTAL_WIDTH = SPACE_SIDE * COLUMNS;
  private static final short TOTAL_HEIGHT = SPACE_SIDE * (TOTAL_ROWS - 3);
  private static final short COLORS = 6;
  // Set the initial and fastest block speeds, in MILLISECONDS PER PIXEL (really an inverse speed)
  private static final short INIT_SPEED = 300;
  private static final short FASTEST = 30;
  private static final long TIME_TO_FASTEST = 1800000;
  private static final int[] COL_POSITIONS = new int[COLUMNS];
  
  private static final Rectangle PLAY_AREA
    = new Rectangle(TOTAL_WIDTH, TOTAL_HEIGHT);
  
  private static Row[] rows = new Row[TOTAL_ROWS];
  private Cursor cursor = new Cursor();
  private long currentSpeed;
  
  // Create a list to monitor temporary events--
  // namely a block-swap, a falling block, or a
  // disappearing block.
  private ArrayList visEvents = new ArrayList();
  
  public void run()
  {
    screen = new ScreenManager();
    try
    {
      DisplayMode displayMode =
        screen.findFirstCompatibleMode(POSSIBLE_MODES);
      screen.setFullScreen(displayMode);
      gameLoop();
    }
    finally
    {
      screen.restoreScreen();
    }
  }
  
  public void gameLoop()
  {
    long startTime = System.currentTimeMillis();
    long currTime = startTime;
    
    // Arrange the block columns at their proper coordinates
    for (int n = 0; n < COL_POSITIONS.length; n++)
    {
      COL_POSITIONS[n] = (int)PLAY_AREA.getX() + (int)(SPACE_SIDE * n);
    }
    
    // Create everything!
    generate();
    
    screen.getFullScreenWindow().addKeyListener(new ArrowListener());
    screen.getFullScreenWindow().setFocusable(true);
    
    currentSpeed = INIT_SPEED;
    
    while (currTime - startTime < DEMO_TIME) // Close the game after a little while
    {
      long elapsedTime = // Counts time passed since last screen draw
        System.currentTimeMillis() - currTime;
      currTime += elapsedTime;
      
      // Speed up over time. Remember, speed is inverted: milliseconds per pixel.
      if (currentSpeed > FASTEST)
      {
      //currentSpeed = (long)((INIT_SPEED - (double)((INIT_SPEED - FASTEST) /
      //                                             TIME_TO_FASTEST) * (currTime -
      //                                                              startTime)));
      
        currentSpeed = (short)(INIT_SPEED - ((System.currentTimeMillis() - startTime) / 30));
      }
        
      // Update the positions of rows/other blocks
      update(elapsedTime);
      
      // draw and update screen
      Graphics2D g = screen.getGraphics();
      draw(g);
      g.dispose();
      screen.update();
      
      // take a nap
      try
      {
        Thread.sleep(20);
      }
      catch (InterruptedException ex) { }
    }
  }
  
  //**********
  // Initial generation
  //**********
  public void generate()
  {
    Random rand = new Random();
    int[] columnLengths = new int[COLUMNS];
    
    // Randomize the initial number of blocks in each column.
    for (int c = 0; c < COLUMNS; c++)
      columnLengths[c] = rand.nextInt(7) + 2;
    
    for (int r = 0; r < rows.length; r++)
    {
      Block[] tempRow = new Block[COLUMNS];
      
      for (int c = 0; c < COLUMNS; c++)
      {
        if (r > columnLengths[c])
          tempRow[c] = null;
        else
        {
          boolean itWorks = false;
          
          while (!itWorks)
          {
            // Try a color for the new block
            int colorNum = rand.nextInt(COLORS);
            
            // Check down and left for a three-in-a-row
            Block onePrev = (c > 2) ? 
              tempRow[c - 1] : null;
            Block twoPrev = (c > 2) ?
              tempRow[c - 2] : null;
            if (!checkForConflict(colorNum, r, c, onePrev, twoPrev))
            {
              itWorks = true;
              tempRow[c] = new Block(colorNum, c);
            }
          }
        }
      }
      
      // Create the row and give it a y-coordinate
      int yPos = TOTAL_HEIGHT - (SPACE_SIDE * r);
      rows[r] = new Row(tempRow, yPos, r);
    }
    
    // Set default cursor position, based on the left-hand square.
    cursor.setLeft(4, 2);
  }
  
  public boolean checkForConflict(int colorNum, int r, int c, 
       Block oneLeft, Block twoLeft)
  {
    boolean result = false;
    
    if (r > 1)
      if (checkDownConflict(colorNum, r, c))
        result = true;
    
    if (c > 1)
      if (checkLeftConflict(colorNum, oneLeft, twoLeft))
        result = true;
    
    return result;
  }
  
  public boolean checkDownConflict(int colorNum, int r, int c)
  {
    boolean result = false;
    
    if (rows[r - 1].getBlock(c) != null && 
          colorNum == rows[r - 1].getBlock(c).getColorNum())
      if (rows[r - 2].getBlock(c) != null &&
            colorNum == rows[r - 2].getBlock(c).getColorNum())
        result = true;
    
    return result;
  }
  
  public boolean checkLeftConflict(int colorNum, Block oneLeft,
                                   Block twoLeft)
  {
    boolean result = false;
    
    if (oneLeft != null &&
          colorNum == oneLeft.getColorNum())
    {
      if (twoLeft != null &&
            colorNum == twoLeft.getColorNum())
        result = true;
    }
    
    return result;
  }
  
  public boolean checkUpConflict(int colorNum, int r, int c)
  {
    boolean result = false;
    
    int oneRowUp = (r + 1) % TOTAL_ROWS;
    int twoRowsUp = (r + 2) % TOTAL_ROWS;
    
    if (rows[oneRowUp].getBlock(c) != null &&
        colorNum == rows[oneRowUp].getBlock(c).getColorNum())
    {
      if (rows[twoRowsUp].getBlock(c) != null &&
          colorNum == rows[twoRowsUp].getBlock(c).getColorNum())
        result = true;
    }
    
    return result;
  }
  
  //**********
  // Update stuff
  //**********
  public void update(long elapsedTime)
  {
    for (Row row : rows)
      row.updateY(elapsedTime);
    
    // Check for blocks that need to disappear
    checkForMarked();
    
    // Update ongoing swaps, falls, and disappearings
    if (!visEvents.isEmpty())
    {
      for (int e = 0; e < visEvents.size(); e++)
      {
        VisualEvent ev = (VisualEvent)visEvents.get(e);
        ev.update(elapsedTime);
      }
    }
    
    // Remove any of those events that are now finished
    if (!visEvents.isEmpty())
    {
      int e = visEvents.size() - 1;
      
      while (e >= 0)
      {
        VisualEvent ev = (VisualEvent)visEvents.get(e);
        if (ev.finished())
          visEvents.remove(e);
        
        e--;
      }
    }
    
    for (Row row : rows)
      row.update();
    
    cursor.setInPlace();
  }
  
  //**********
  // Draw the screen
  //**********
  public void draw(Graphics2D g)
  {
    g.setColor(Color.black);
    g.fillRect((int)(PLAY_AREA.getX()), (int)(PLAY_AREA.getY()),
               800, 600);
    
    for (Row row : rows)
      row.draw(g);
    
    if (!visEvents.isEmpty())
    {
      for (int e = 0; e < visEvents.size(); e++)
      {
        VisualEvent ev = (VisualEvent)visEvents.get(e);
        ev.draw(g);
      }
    }
    
    cursor.draw(g);
  }
  
  //******
  // Check one block for three-in-a-rows in all four directions
  //******
  public void checkOneBlock(int row, int col, boolean[] skipDirections)
  {
    boolean allMatches;
    int scan;
    int consecs;
    Block mainBlock = rows[row].getBlock(col);
    int colorNum = mainBlock.getColorNum();
    ArrayList blocksMarked;
    
    
    // Check up and down
    consecs = 0;
    blocksMarked = new ArrayList();
    
    //
    // Scan upwards
    //
    if (!skipDirections[0])
    {
      allMatches = true;
      scan = (row + 1) % TOTAL_ROWS;
      while (allMatches && rows[scan].getVisible())
      {
        Block block = rows[scan].getBlock(col);
        if (block != null && block.getColorNum() == colorNum)
        {
          consecs++;
          if (!block.getMarked())
          {
            block.setMarked(true);
            blocksMarked.add(block);
          }
        }
        else
          allMatches = false;
        
        scan = (scan + 1) % TOTAL_ROWS;
      }
    }
    
    
    //
    // Scan downwards
    //
    if (!skipDirections[2])
    {
      allMatches = true;
      scan = row - 1;
      while (scan < 0)
        scan += TOTAL_ROWS;
      while (allMatches && rows[scan].getVisible())
      {
        Block block = rows[scan].getBlock(col);
        if (block != null && block.getColorNum() == colorNum)
        {
          consecs++;
          if (!block.getMarked())
          {
            block.setMarked(true);
            blocksMarked.add(block);
          }
        }
        else
          allMatches = false;
        
        scan--;
        while (scan < 0)
          scan += TOTAL_ROWS;
      }
    }
    
    
    //
    // Process U/D results
    //
    if (consecs > 1) // Check whether we have a three-in-a-row
      mainBlock.setMarked(true);
    else
    {
      for (int b = 0; b < blocksMarked.size(); b++)
      {
        // Unmark any newly marked blocks if there's no three-in-a-row
        Block block = (Block)blocksMarked.get(b);
        block.setMarked(false);
      }
    }
    
    
    // Check left and right
    consecs = 0;
    blocksMarked = new ArrayList();
    
    //
    // Scan to the right
    //
    if (!skipDirections[1])
    {
      allMatches = true;
      scan = col + 1;
      while (allMatches && scan < COLUMNS)
      {
        Block block = rows[row].getBlock(scan);
        if (block != null && block.getColorNum() == colorNum)
        {
          consecs++;
          if (!block.getMarked())
          {
            block.setMarked(true);
            blocksMarked.add(block);
          }
        }
        else
          allMatches = false;
        
        scan++;
      }
    }
    
    //
    // Scan to the left
    //
    if (!skipDirections[3])
    {
      allMatches = true;
      scan = col - 1;
      while (allMatches && scan >= 0)
      {
        Block block = rows[row].getBlock(scan);
        if (block != null && block.getColorNum() == colorNum)
        {
          consecs++;
          if (!block.getMarked())
          {
            block.setMarked(true);
            blocksMarked.add(block);
          }
        }
        else
          allMatches = false;
      
        scan--;
      }
    }
    
    //
    // Process L/R results
    //
    
    if (consecs > 1) // Check whether we have a three-in-a-row
      mainBlock.setMarked(true);
    else
    {
      for (int b = 0; b < blocksMarked.size(); b++)
      {
        // Unmark any newly marked blocks if there's no three-in-a-row
        Block block = (Block)blocksMarked.get(b);
        block.setMarked(false);
      }
    }
  }
  
  //*******
  // Check for blocks marked for destruction, and make them disappear
  //*******
  public void checkForMarked()
  {
    for (int row = 0; row < TOTAL_ROWS; row++)
    {
      if (rows[row].getVisible())
      {
        for (int col = 0; col < COLUMNS; col++)
        {
          Block block = rows[row].getBlock(col);
          if (block != null && block.getMarked())
            visEvents.add(new Disappear(block, row, col));
        }
      }
    }
  }
  
  //*************
  // Check upwards for any stack of blocks that needs to fall
  //*************
  public void checkUpCol(int startRow, int column)
  {
    int countUp = 0;
    int scanRow;
    do 
    {
      countUp++;
      scanRow = (startRow + countUp) % TOTAL_ROWS;
    } while (rows[scanRow].getBlock(column) != null && 
             rows[scanRow].getVisible());
        
    if (countUp > 1)
    {
      int[] newCols = {column};
      int[] newRows = new int[countUp - 1];
      for (int r = 0; r < (countUp - 1); r++)
      {
        int num = (startRow + r + 1) % TOTAL_ROWS;
        newRows[r] = num;
      }
          
      visEvents.add(new Falling(newRows, newCols, true));
    }
  }
  
//********************************************
// Class Row
//********************************************
  
  private class Row
  {
    private int y;
    private long timeSinceMove;
    private long speed = INIT_SPEED; // in milliseconds per pixel
    private Block[] blocks = new Block[COLUMNS];
    private boolean isVisible = true;
    private boolean hasCursor = false;
    private boolean available[] = new boolean[COLUMNS];
    private boolean wasJustUnplayable = false;
    private int row;
    
    public Row(Block[] tempRow, int yPos, int row)
    {
      y = yPos;
      blocks = tempRow;
      this.row = row;
      
      for (int b = 0; b < blocks.length; b++)
      {
        if (blocks[b] != null)
          blocks[b].setY(y + 1);
        
        available[b] = true;
      }
    }
    
    public void setYPos(int yPos)
    {
      y = yPos;
    }
    
    public void update()
    {
        for (Block block : blocks)
        {
          if (block != null)
          {
            block.setY(y + 1);
            block.update();
          }
        }
      
    }
    
    public void updateY(long elapsedTime)
    {
      speed = currentSpeed;
      
      timeSinceMove += elapsedTime;
      if (timeSinceMove > speed)
      {
        y -= (timeSinceMove / speed);
        timeSinceMove %= speed;
      }
      
      // Check if hitting top of screen
      if (y < PLAY_AREA.getY())
      {
        y += (TOTAL_ROWS * SPACE_SIDE);
        resetRow();
        if (hasCursor)
        {
          cursor.moveDown();
          hasCursor = false;
        }
        //passRows();
      }
      
      // Check if below play area
      if (y > (int)(PLAY_AREA.getY() + TOTAL_HEIGHT))
      {
        isVisible = false;
        wasJustUnplayable = true;
        if (hasCursor)
        {
          cursor.moveUp();
          hasCursor = false;
        }
      }
      else
      {
        isVisible = true;
        if (wasJustUnplayable)
        {
          wasJustUnplayable = false;
          boolean[] skipDirections = {false, true, true, true};
          for (int c = 0; c < COLUMNS; c++)
            checkOneBlock(this.row, c, skipDirections);
        }
      }
    }
    
    public void draw(Graphics g)
    {
      if (isVisible)
      {
        for (Block block : blocks)
        {
          if (block != null)
            block.draw(g);
        }
      }
    }
    
    public void resetRow()
    {
      for (int c = 0; c < COLUMNS; c++)
      {
        Random rand = new Random();
        int colorNum = rand.nextInt(COLORS);
        
        if (c > 1)
        {
          while (checkLeftConflict(colorNum, blocks[c - 1], blocks[c - 2]) ||
                 checkUpConflict(colorNum, this.row, c))
          {
            colorNum = rand.nextInt(COLORS);
          }
        }
        blocks[c] = new Block(colorNum, c);
      }
    }
    
    public Block getBlock(int column)
    {
      return blocks[column];
    }
    
    public void setBlock(Block block, int column)
    {
      blocks[column] = block;
    }
    
    public boolean hasBlock(int column)
    {
      return (blocks[column] == null) ? false : true;
    }
    
    public void setHasCursor(boolean cond)
    {
      hasCursor = cond;
    }
    
    public int getY()
    {
      return y;
    }
    
    public boolean getVisible()
    {
      return isVisible;
    }
    
    
    // *** Check for availability of a space for an action ***
    public boolean getAvailable(int column)
    {
      return available[column];
    }
    
    public void setAvailable(boolean value, int column)
    {
      available[column] = value;
    }
  }
  
  //********************************************
  // Class Block
  //********************************************
  
  private class Block extends Polygon
  {
    private Color color;
    private int colorN;
    private int x, y;
    private boolean marked; // for destruction
    
    public Block(int colorNum, int column)
    {
      super();
      
      this.colorN = colorNum;
      
      switch(colorNum)
      {
        case 0:
          color = Color.blue;
          break;
        case 1:
          color = Color.magenta;
          break;
        case 2:
          color = Color.red;
          break;
        case 3:
          color = Color.yellow;
          break;
        case 4:
          color = Color.cyan;
          break;
        case 5:
          color = Color.green;
          break;
      }
      
      x = (column * SPACE_SIDE) + 1;
    }
    
    public void setX(int xPos)
    {
      x = xPos;
    }
    
    public void setY(int yPos)
    {
      y = yPos;
    }
    
    public int getX()
    {
      return x;
    }
    
    public int getY()
    {
      return y;
    }
    
    public void update()
    {
      reset();
      addPoint(x, y);
      addPoint(x + BLOCK_SIDE - 1, y);
      addPoint(x + BLOCK_SIDE - 1, y + BLOCK_SIDE - 1);
      addPoint(x, y + BLOCK_SIDE - 1);
    }
    
    public void draw(Graphics g)
    {
      g.setColor(this.color);
      g.fillPolygon(this);
    }
    
    public int getColorNum()
    {
      return colorN;
    }
    
    public Color getColor()
    {
      return this.color;
    }
    
    public void setColor(Color c)
    {
      this.color = c;
    }
    
    public boolean getMarked()
    {
      return this.marked;
    }
    
    public void setMarked(boolean m)
    {
      this.marked = m;
    }
  }
  
  //*******************************************
  //             Class Cursor
  //*******************************************
  
  private class Cursor
  {
    private int row;
    private int leftColumn;
    
    private Polygon[][] cursors = new Polygon[2][];
    
    public Cursor()
    {
      for (int i = 0; i < 2; i++)
        cursors[i] = new Polygon[4];
    }
    
    public void moveDown()
    {
      int compareNum = (row > 0) ? row - 1 : TOTAL_ROWS - 1;
      if (rows[compareNum].getVisible())
      {
        rows[row].setHasCursor(false);
        row = compareNum;
        setInPlace();
      }
    }
    
    public void moveUp()
    {
      int compareNum = (row < TOTAL_ROWS - 1) ? row + 1 : 0;
      if (rows[compareNum].getVisible())
      {
        rows[row].setHasCursor(false);
        row = compareNum;
        setInPlace();
      }
    }
    
    public void moveLeft()
    {
      if (leftColumn > 0)
      {
        leftColumn--;
        setInPlace();
      }
    }
    
    public void moveRight()
    {
      if (leftColumn < 4)
      {
        leftColumn++;
        setInPlace();
      }
    }
    
    public void setLeft(int row, int column)
    {
      this.row = row;
      this.leftColumn = column;
    }
    
    //*********** SWAP ************* 
    //*********** CODE *************
    
    public void swap()
    {
      if (rows[this.row].getAvailable(leftColumn) &&
          rows[this.row].getAvailable(leftColumn + 1))
        // Make sure there's at least one block to move.
        if (!(rows[this.row].getBlock(leftColumn) == null &&
              rows[this.row].getBlock(leftColumn + 1) == null))
          visEvents.add(new Swap(this.row, this.leftColumn));
    }
    
    // This is a function that was created to test the block-destruction
    // code, and is triggered by a keystroke of 'D'
    public void destroy()
    {
      if (rows[this.row].getBlock(leftColumn) != null)
      {
        visEvents.add(new Disappear(rows[this.row].getBlock(leftColumn),
                                    this.row, leftColumn));
      }
          
      if (rows[this.row].getBlock(leftColumn + 1) != null)
      {
        visEvents.add(new Disappear(rows[this.row].getBlock(leftColumn + 1),
                                    this.row, leftColumn + 1));
      }
      
      // Add a check for any blocks that need to fall, and call
      // instantiate the Falling class appropriately.
    }
    
    public void setInPlace()
    {
      if (row == TOTAL_ROWS)
        row = 0;
      else if (row == -1)
        row = TOTAL_ROWS - 1;
      
      
      rows[row].setHasCursor(true);
      
      int[] startX = {((int)(PLAY_AREA.getX()) + (leftColumn * SPACE_SIDE)),
                      ((int)(PLAY_AREA.getX()) + ((leftColumn + 1) * SPACE_SIDE))};
      int startY = rows[row].getY();
      final int LONGW = BLOCK_SIDE / 3;
      final int SHORTW = BLOCK_SIDE / 6;
      
      for (int i = 0; i < 2; i++)
      {
        for (int p = 0; p < 4; p++)
        {
          if (cursors[i][p] != null)
            cursors[i][p].reset();
          else
            cursors[i][p] = new Polygon();
        }
        
        cursors[i][0].addPoint(startX[i], startY);
        cursors[i][0].addPoint(startX[i] + LONGW, startY);
        cursors[i][0].addPoint(startX[i] + LONGW, startY + SHORTW);
        cursors[i][0].addPoint(startX[i] + SHORTW, startY + SHORTW);
        cursors[i][0].addPoint(startX[i] + SHORTW, startY + LONGW);
        cursors[i][0].addPoint(startX[i], startY + LONGW);
        
        cursors[i][1].addPoint(startX[i] + SPACE_SIDE - 1, startY);
        cursors[i][1].addPoint(startX[i] + SPACE_SIDE - 1 - LONGW, startY);
        cursors[i][1].addPoint(startX[i] + SPACE_SIDE - 1 - LONGW, startY + SHORTW);
        cursors[i][1].addPoint(startX[i] + SPACE_SIDE - 1 - SHORTW, startY + SHORTW);
        cursors[i][1].addPoint(startX[i] + SPACE_SIDE - 1 - SHORTW, startY + LONGW);
        cursors[i][1].addPoint(startX[i] + SPACE_SIDE - 1, startY + LONGW);
        
        cursors[i][2].addPoint(startX[i] + SPACE_SIDE - 1, startY + SPACE_SIDE - 1);
        cursors[i][2].addPoint(startX[i] + SPACE_SIDE - 1 - LONGW, startY + SPACE_SIDE - 1);
        cursors[i][2].addPoint(startX[i] + SPACE_SIDE - 1 - LONGW, startY + SPACE_SIDE - 1 - SHORTW);
        cursors[i][2].addPoint(startX[i] + SPACE_SIDE - 1 - SHORTW, startY + SPACE_SIDE - 1 - SHORTW);
        cursors[i][2].addPoint(startX[i] + SPACE_SIDE - 1 - SHORTW, startY + SPACE_SIDE - 1 - LONGW);
        cursors[i][2].addPoint(startX[i] + SPACE_SIDE - 1, startY + SPACE_SIDE - 1 - LONGW);
        
        cursors[i][3].addPoint(startX[i], startY + SPACE_SIDE - 1);
        cursors[i][3].addPoint(startX[i] + LONGW, startY + SPACE_SIDE - 1);
        cursors[i][3].addPoint(startX[i] + LONGW, startY + SPACE_SIDE - 1 - SHORTW);
        cursors[i][3].addPoint(startX[i] + SHORTW, startY + SPACE_SIDE - 1 - SHORTW);
        cursors[i][3].addPoint(startX[i] + SHORTW, startY + SPACE_SIDE - 1 - LONGW);
        cursors[i][3].addPoint(startX[i], startY + SPACE_SIDE - 1 - LONGW);
      }
    }
    
    public void draw(Graphics g)
    {
      g.setColor(Color.white);
      for (int i = 0; i < 2; i++)
      {
        for (int p = 0; p < 4; p++)
        {
          g.fillPolygon(cursors[i][p]);
        }
      }
    }
  }
  
  //************************************************
  //           Class ArrowListener
  //************************************************
  private class ArrowListener implements KeyListener
  {
    private long storeSpeed;
    boolean spacePressed;
    
    public void keyPressed(KeyEvent e)
    {
      int keyCode = e.getKeyCode();
      
      switch (keyCode)
      {
        case KeyEvent.VK_UP:
          cursor.moveUp();
          break;
        case KeyEvent.VK_DOWN:
          cursor.moveDown();
          break;
        case KeyEvent.VK_LEFT:
          cursor.moveLeft();
          break;
        case KeyEvent.VK_RIGHT:
          cursor.moveRight();
          break;
        case KeyEvent.VK_ENTER:
          cursor.swap();
          break;
        case KeyEvent.VK_SPACE:
          //if (spacePressed)
          //{
          //  storeSpeed = currentSpeed;
          //  currentSpeed = FASTEST;
          //}
          //spacePressed = true;
          break;
        default:
          e.consume();
      }
      e.consume();
    }
    
    public void keyReleased(KeyEvent e)
    {
      int keyCode = e.getKeyCode();
      
      if (keyCode == KeyEvent.VK_SPACE)
      {
        cursor.destroy();
        
        //currentSpeed = storeSpeed;
        //spacePressed = false;
      }
      
      e.consume();
    }
    
    public void keyTyped(KeyEvent e)
    {
      e.consume();
    }
  }
  
  //*******************************************************************
  //            Class Swap
  //*******************************************************************
  private class Swap implements VisualEvent
  {
    private int row;
    private int[] cols = new int[2];
    private int timeSoFar;
    private Block[] blocks = new Block[2];
    private int[] initX = new int[2];
    private boolean finished = false;
    
    public static final int SWAP_TIME = 150;
    
    public Swap(int row, int leftCol)
    {
      this.row = row;
      this.cols[0] = leftCol;
      this.cols[1] = leftCol + 1;
      
      for (int p = 0; p < 2; p++)
      {
        blocks[p] = rows[this.row].getBlock(cols[p]);
        rows[this.row].setBlock(null, cols[p]);
        initX[p] = cols[p] * SPACE_SIDE + 1;
        rows[this.row].setAvailable(false, cols[p]);
      }
    }
    
    public void update(long elapsedTime)
    {
      timeSoFar += elapsedTime;
      if (timeSoFar >= SWAP_TIME)
        this.finish();
      else
      {
        int distance = (int)(((double)timeSoFar / SWAP_TIME)
                                          * SPACE_SIDE);
        
        if (blocks[0] != null)
          blocks[0].setX(initX[0] + distance);
        if (blocks[1] != null)
          blocks[1].setX(initX[1] - distance);
        
        for (Block block : blocks)
        {
          if (block != null)
          {
            block.setY(rows[this.row].getY());
            block.update();
          }
        }
      }
    }
    
    public void draw(Graphics g)
    {
      for (Block block : blocks)
        if (block != null)
          block.draw(g);
    }
    
    public void finish()
    {
      if (blocks[0] != null)
      {
        blocks[0].setX(initX[1]);
        rows[this.row].setBlock(blocks[0], cols[1]);
      }
      
      if (blocks[1] != null)
      {
      blocks[1].setX(initX[0]);
      rows[this.row].setBlock(blocks[1], cols[0]);
      }
      
      // Check for blocks to destroy
      // If the two blocks are the same color,
      // no need for destruction.
      if (!(blocks[0] != null && blocks[1] != null && 
            blocks[0].getColor().equals(blocks[1].getColor())))
      {
        boolean[][] skipDirections = { {false, true, false, false},
          {false, false, false, true} };
        
        if (blocks[0] != null)
          checkOneBlock(this.row, this.cols[1], skipDirections[1]);
        if (blocks[1] != null)
          checkOneBlock(this.row, this.cols[0], skipDirections[0]);
      }
      
      
      
      // * * * * * *
      // Check whether
      // stuff needs
      // to fall
      // * * * * * *
      
      
      // blocks[0]
      if (blocks[0] == null)
      {
        checkUpCol(this.row, cols[1]);
      }
      // blocks[1]
      if (blocks[1] == null)
      {
        checkUpCol(this.row, cols[0]);
      }
      
      // Under blocks[0]
      if (blocks[0] != null)
      {
        int targetRow = this.row - 1;
        while (targetRow < 0)
          targetRow += TOTAL_ROWS;
        
        if (rows[targetRow].getBlock(this.cols[1]) == null)
        {
          int newRows[] = {this.row};
          int newCols[] = {this.cols[1]};
          visEvents.add(new Falling(newRows, newCols, true));
        }
      }
      // Under blocks[1]
      else if (blocks[1] != null)
      {
        int targetRow = this.row - 1;
        while (targetRow < 0)
          targetRow += TOTAL_ROWS;
        
        if (rows[targetRow].getBlock(this.cols[0]) == null)
        {
          int newRows[] = {this.row};
          int newCols[] = {this.cols[0]};
          visEvents.add(new Falling(newRows, newCols, true));
        }
      }
      
      
      
      
      for (Block block : blocks)
      {
        if (block != null)
        {
          block.setY(rows[this.row].getY());
          block.update();
          block = null;
        }
      }
      
      for (int c = 0; c < 2; c++)
        rows[this.row].setAvailable(true, cols[c]);
      
      //visEvents.remove(visEvents.indexOf(this));
      this.finished = true;
    }
    
    public boolean finished()
    {
      return this.finished;
    }
  }
  
  //*******************************************************************
  //                 Class Disappear
  //*******************************************************************
  private class Disappear implements VisualEvent
  {
    private Block block;
    
    private int x, y;
    private int row, col;
    private int timeSoFar;
    private int initRed, initGreen, initBlue;
    private int red, green, blue;
    private boolean finished = false;
    
    public static final int DIS_TIME = 300;
    
    public Disappear(Block b, int row, int col)
    {
      x = b.getX();
      y = b.getY();
      this.row = row;
      this.col = col;
      rows[this.row].setBlock(null, this.col);
      block = b;
      initBlue = block.getColor().getBlue();
      initGreen = block.getColor().getGreen();
      initRed = block.getColor().getRed();
    }
    
    public void update(long elapsedTime)
    {
      timeSoFar += elapsedTime;
      if (timeSoFar <= DIS_TIME)
      {
        block.setY(rows[this.row].getY());
        block.update();
        block.setColor(block.getColor().darker());
      }
      else
        this.finish();
    }
    
    public void draw(Graphics g)
    {
      block.draw(g);
    }
    
    public void finish()
    {
      block = null;
      checkUpCol(this.row, this.col);
      //visEvents.remove(visEvents.indexOf(this));
      this.finished = true;
    }
    
    public boolean finished()
    {
      return this.finished;
    }
  }
  
  //*******************************************************************
  //                Class Falling
  //*******************************************************************
  private class Falling implements VisualEvent
  {
    private int initBottomRow;
    private int nextRow;
    private Block[][] blocks;
    private int[] itsRows;
    private int[] itsCols;
    private boolean firstFall;
    private boolean finished = false;
    
    private int timeSoFar;
    
    public static final int FALL_TIME = 100;
    
    public Falling(int[] newRows, int[] newCols, boolean first)
    {
      blocks = new Block[rows.length][];
      itsRows = newRows;
      itsCols = newCols;
      firstFall = first;
      
      for (int r = 0; r < itsRows.length; r++)
      {
        blocks[r] = new Block[itsCols.length];
        
        for (int c = 0; c < itsCols.length; c++)
        {
          blocks[r][c] = rows[itsRows[r]].getBlock(itsCols[c]);
          rows[itsRows[r]].setBlock(null, itsCols[c]);
        }
      }
    }
    
    
    public void update(long elapsedTime)
    {
      timeSoFar += elapsedTime;
      
      for (int r = 0; r < itsRows.length; r++)
      {
        for (int c = 0; c < itsCols.length; c++)
        {
          if (timeSoFar <= this.FALL_TIME)
          {
            if (blocks[r][c] != null)
            {
              int distance = (int)(((double)(timeSoFar) / (this.FALL_TIME)) 
                            * SPACE_SIDE);
            
              blocks[r][c].setY(rows[itsRows[r]].getY() + 1 + distance);
              blocks[r][c].update();
            }
          }
          else if (timeSoFar > (this.FALL_TIME))
            this.finish();
          
        }
      }
    }
    
    
    public void draw(Graphics g)
    {
      for (int r = 0; r < itsRows.length; r++)
      {
        for (int c = 0; c < itsCols.length; c++)
        {
          if (blocks[r][c] != null)
            blocks[r][c].draw(g);
        }
      }
    }
    
    public void finish()
    {
      boolean[] allDirections = {false, false, false, false};
      
      for (int c = 0; c < itsCols.length; c++)
      {
        int targetRow = itsRows[0] - 2;
        while (targetRow < 0)
          targetRow += TOTAL_ROWS;
        if (rows[targetRow].getBlock(itsCols[c]) == null &&
                         rows[targetRow].getVisible())
        {
          int[] newCols = {itsCols[c]};
          int[] newRows = new int[itsRows.length];
          for (int r = 0; r < itsRows.length; r++)
          {
            int newTargetRow = itsRows[r] - 1;
            while (newTargetRow < 0)
              newTargetRow += TOTAL_ROWS;
            
            rows[newTargetRow].setBlock(blocks[r][c], itsCols[c]);
            newRows[r] = newTargetRow;
            
            //blocks[r][c] = null;
          }
          
          visEvents.add(new Falling(newRows, newCols, false));
        }
        else
        {
          for (int r = 0; r < itsRows.length; r++)
          {
            int newTargetRow = itsRows[r] - 1;
            while (newTargetRow < 0)
              newTargetRow += TOTAL_ROWS;
            
            rows[newTargetRow].setBlock(blocks[r][c], itsCols[c]);
            
            // Check for matches
            checkOneBlock(newTargetRow, itsCols[c], allDirections);
          }
        }
        
        
      }
      
      this.finished = true;
    }
    
    
    public int[] getRows()
    {
      return itsRows;
    }
    
    public int[] getCols()
    {
      return itsCols;
    }
    
    public boolean finished()
    {
      return this.finished;
    }
  }
  
  //*******************************************************************
  //            Abstract Class VisualEvent
  //*******************************************************************
  private interface VisualEvent
  {
    abstract public void update(long elapsedTime);
    abstract public void draw(Graphics g);
    abstract public boolean finished();
  }
}