package jigsaw.client.backend.figure;

import java.util.Random;

/**
 * Класс для генерации фигур по ТЗ.
 */
public class JigsawFiguresSeeder {
    // Переменные для чуть более удобного изображения фигур в коде.
    private static final boolean F = false;
    private static final boolean T = true;

    /**
     * Получить случайное изображение фигуры из существующих.
     * @return Случайное изображение фигуры из существующих.
     */
    public static boolean[][] getRandomFigureImage() {
        int randomIndex = new Random().nextInt(figureImages.length);
        return figureImages[randomIndex];
    }

    /**
     * Изображения для фигур, описанные в ТЗ.
     */
    private static final boolean[][][] figureImages = {
            // Первые 4 фигуры (первая строка из ТЗ).
            {
                    {T, T, F},
                    {T, F, F},
                    {T, F, F},
            },
            {
                    {T, F, F},
                    {T, T, T},
                    {F, F, F},
            },
            {
                    {F, T, F},
                    {F, T, F},
                    {T, T, F},
            },
            {
                    {T, T, T},
                    {F, F, T},
                    {F, F, F},
            },

            // Строка 2.
            {
                    {T, T, F},
                    {F, T, F},
                    {F, T, F},
            },
            {
                    {F, F, T},
                    {T, T, T},
                    {F, F, F},
            },
            {
                    {T, F, F},
                    {T, F, F},
                    {T, T, F},
            },
            {
                    {T, T, T},
                    {T, F, F},
                    {F, F, F},
            },

            // Строка 3.
            {
                    {T, F, F},
                    {T, T, F},
                    {F, T, F},
            },
            {
                    {F, T, T},
                    {T, T, F},
                    {F, F, F},
            },
            {
                    {F, T, F},
                    {T, T, F},
                    {T, F, F},
            },
            {
                    {T, T, F},
                    {F, T, T},
                    {F, F, F},
            },

            // Строка 4.
            {
                    {F, F, T},
                    {F, F, T},
                    {T, T, T},
            },
            {
                    {T, F, F},
                    {T, F, F},
                    {T, T, T},
            },
            {
                    {T, T, T},
                    {T, F, F},
                    {T, F, F},
            },
            {
                    {T, T, T},
                    {F, F, T},
                    {F, F, T},
            },

            // Строка 5.
            {
                    {F, T, F},
                    {F, T, F},
                    {T, T, T},
            },
            {
                    {T, T, T},
                    {F, T, F},
                    {F, T, F},
            },
            {
                    {T, F, F},
                    {T, T, T},
                    {T, F, F},
            },
            {
                    {F, F, T},
                    {T, T, T},
                    {F, F, T},
            },

            // Строка 6.
            {
                    {T, T, T},
                    {F, F, F},
                    {F, F, F},
            },
            {
                    {T, F, F},
                    {T, F, F},
                    {T, F, F},
            },
            {
                    {T, F, F},
                    {F, F, F},
                    {F, F, F},
            },

            // Строка 7.
            {
                    {T, T, F},
                    {T, F, F},
                    {F, F, F},
            },
            {
                    {T, T, F},
                    {F, T, F},
                    {F, F, F},
            },
            {
                    {F, T, F},
                    {T, T, F},
                    {F, F, F},
            },
            {
                    {T, F, F},
                    {T, T, F},
                    {F, F, F},
            },

            // Строка 8.
            {
                    {T, F, F},
                    {T, T, F},
                    {T, F, F},
            },
            {
                    {T, T, T},
                    {F, T, F},
                    {F, F, F},
            },
            {
                    {F, T, F},
                    {T, T, F},
                    {F, T, F},
            },
            {
                    {F, T, F},
                    {T, T, T},
                    {F, F, F},
            },
    };
}
