
## Bubble Sets

This is an implementation of [bubble sets] [1]
without the use of external libraries.

This project can be build in eclipse as well as with Maven.
Use `mvn install` to generate jars in the *target/* directory.
The *Bubble-Sets-X.X.X.jar* contains the API for the bubble sets
and *Bubble-Sets-X.X.X-example.jar* contains a little example program
showing the bubble sets. Alternatively the project can be used as dependency
in other Maven projects (see [below] (#maven-integration)).

To get started, you can use the following code:

    // a list of groups of rectangles --
    // bubble set will try to create an outline with
    // as little overlap between the groups as possible
    List<Rectangle2D[]> items = ...;

    // using bubble set outlines
    SetOutline setOutline = new BubbleSet();

    // make the outlines smooth
    AbstractShapeGenerator shapeGenerator = new BezierShapeGenerator(setOutline);

    // generate shapes for each group
    // the shapes can be drawn by a Graphics object
    // as passed by a component's paint method
    Shape[] shapes = shapeGenerator.createShapesFor(items);

A more advanced example uses groups which can also
define lines that guide the iso-contour. It also shows
a possible use within a paint method:

    @Override
    public void paint(final Graphics gfx) {
        // using graphics 2d
        Graphics2D g = (Graphics2D) gfx;

        // the list of groups
        List<Group> items = null;

        // use bubble sets and make the outlines smooth and less complex
        // to draw by simplifying the raw points
        AbstractShapeGenerator shapeGenerator = new ShapeSimplifier(
                new BSplineShapeGenerator(new ShapeSimplifier(
                        new PolygonShapeGenerator(new BubbleSet()))), 2.0);

        // generate shapes for each group
        Shape[] shapes = shapeGenerator.createShapesForGroups(items);

        for (Shape shape : shapes) {
            // drawing the content
            g.setColor(Color.ORANGE);
            g.fill(shape);

            // and then the outlines
            g.setColor(Color.BLACK);
            g.draw(shape);
        }
    }

The following set outlines are available:

- `setvis.ch.ConvexHull`
- `setvis.bubbleset.BubbleSet`

And the following shape generators:

- `setvis.shape.PolygonShapeGenerator`
- `setvis.shape.BezierShapeGenerator`
- `setvis.shape.BSplineShapeGenerator`
- `setvis.shape.ShapeSimplifier` (reduces the shape complexity)

### Maven Integration

In order to use bubble sets within a Maven project you can use the following dependency
(in the `<dependencies>` section)::

    <dependency>
      <groupId>joschi-mvn</groupId>
      <artifactId>Bubble-Sets</artifactId>
      <version>0.0.1</version>
    </dependency>

However, this requires an additional repository in the repositories section (`<repositories>`) of the pom.xml file:

    <repository>
        <id>joschi</id>
        <url>http://josuakrause.github.io/info/mvn-repo/releases</url>
    </repository>

[1]: http://faculty.uoit.ca/collins/research/bubblesets/ "Collins, Christopher; Penn, Gerald; Carpendale, Sheelagh. Bubble Sets: Revealing Set Relations over Existing Visualizations. In IEEE Transactions on Visualization and Computer Graphics (Proceedings of the IEEE Conference on Information Visualization (InfoVis '09)), 15(6): November-December, 2009."
