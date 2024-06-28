/*
 *  Copyright 2024 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.rps.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.stream.IntStream;

/**
 * Custom view that renders the terrain of an intransitive ecosystem (e.g. Rock-Paper-Scissors) as a
 * lattice or grid. All cells in the grid are occupied by members of the competing breeds, and each
 * is displayed as a circle filled with a color specific to that breed.
 */
public class TerrainView extends View {

  private static final float COLOR_SATURATION = 1f;
  private static final float COLOR_VALUE = 1f;

  private Paint[] breedPaints;
  private int[][] terrain;

  /**
   * Chains to the corresponding superclass constructor to initialize the inherited state.
   *
   * @param context Android context in which this instance is created.
   */
  public TerrainView(Context context) {
    super(context);
  }

  /**
   * Chains to the corresponding superclass constructor to initialize the inherited state.
   *
   * @param context Android context in which this instance is created.
   * @param attrs Layout attributes.
   */
  public TerrainView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Chains to the corresponding superclass constructor to initialize the inherited state.
   *
   * @param context Android context in which this instance is created.
   * @param attrs Layout attributes.
   * @param defStyleAttr Style attributes.
   */
  public TerrainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * Chains to the corresponding superclass constructor to initialize the inherited state.
   *
   * @param context Android context in which this instance is created.
   * @param attrs Layout attributes.
   * @param defStyleAttr Style attributes.
   * @param defStyleRes Style resource.
   */
  public TerrainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  /**
   * Computes the dimensions of this instance, setting them to be those of the largest square that
   * conforms to the values of the {@code android:layout_width} and {@code android:layout_height}
   * attributes set in the layout.
   *
   * @param widthMeasureSpec  horizontal space requirements as imposed by the parent. The
   *                          requirements are encoded with {@link android.view.View.MeasureSpec}.
   * @param heightMeasureSpec vertical space requirements as imposed by the parent. The requirements
   *                          are encoded with {@link android.view.View.MeasureSpec}.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = getSuggestedMinimumWidth();
    int height = getSuggestedMinimumHeight();
    width =
        resolveSizeAndState(getPaddingLeft() + getPaddingRight() + width, widthMeasureSpec, 0);
    height =
        resolveSizeAndState(getPaddingTop() + getPaddingBottom() + height, heightMeasureSpec, 0);
    int size = Math.max(width, height);
    setMeasuredDimension(size, size);
  }

  /**
   * Iterates over the terrain (specified as an argument to {@link #setTerrain(int[][])}, rendering
   * its contents to the {@code canvas}. Each row of the terrain array is rendered as a row of
   * circles, squares, or rounded rectangles, each drawn with the {@link Paint} instance
   * corresponding to the breed number of the corresponding element in the terrain.
   *
   * @param canvas Drawing surface.
   */
  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    if (terrain != null && breedPaints != null) {

      float cellSize =
          Math.min((float) getWidth() / terrain[0].length, (float) getHeight() / terrain.length);

      ShapePainter painter = (cellSize < 20) ? Canvas::drawRect : Canvas::drawOval;

      for (int rowIndex = 0; rowIndex < terrain.length; rowIndex++) {
        float rowOffset = rowIndex * cellSize;
        for (int colIndex = 0; colIndex < terrain[rowIndex].length; colIndex++) {
          float colOffset = colIndex * cellSize;
          painter.paint(canvas, colOffset, rowOffset, colOffset + cellSize, rowOffset + cellSize,
              breedPaints[terrain[rowIndex][colIndex]]);
        }
      }

    }
  }

  /**
   * Constructs an array of {@link Paint} instances with a length equal to {@code numBreeds}. The
   * instance at a given position in of the array will be used when drawing each individual of the
   * breed identified by the same number as that position. For example, the {@link Paint} instance
   * in position 2 will be used to draw all members of breed 2 in the grid.
   * <p>All of the color values in the array are specified using the Hue-Saturation-Value (HSV)
   * color model: all have the same saturation and value, while the hues are distributed evenly
   * around the 360&#176; of the color wheel, {@code (360 / numBreeds)}&#176; apart.</p>
   *
   * @param numBreeds Initial number of competing breeds in the ecosystem.
   */
  public void setNumBreeds(int numBreeds) {
    float colorWidth = 360f / numBreeds;
    breedPaints = IntStream.range(0, numBreeds)
        .mapToObj((breed) -> {
          Paint paint = new Paint();
          paint.setColor(
              Color.HSVToColor(new float[]{colorWidth * breed, COLOR_SATURATION, COLOR_VALUE}));
          return paint;
        })
        .toArray(Paint[]::new);
  }

  /**
   * Sets the {@code int[][]} terrain to be rendered, as described in {@link #onDraw(Canvas)}. The
   * value of each element in the terrain is the breed number of the individual occupying that
   * position in the terrain.
   *
   * @param terrain {@code int[][]} to be rendered.
   */
  public void setTerrain(int[][] terrain) {
    this.terrain = terrain;
  }

  @FunctionalInterface
  private interface ShapePainter {

    void paint(Canvas canvas, float left, float top, float right, float bottom, Paint paint);

  }

}
