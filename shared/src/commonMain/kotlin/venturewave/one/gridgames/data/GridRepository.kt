package venturewave.one.gridgames.data

import venturewave.one.gridgames.model.CalibratedGrid
import venturewave.one.gridgames.model.GridTarget

/**
 * Repository for storing and retrieving the calibrated grid
 * Simple in-memory implementation for now
 */
object GridRepository {
    private var calibratedGrid: CalibratedGrid? = null

    /**
     * Save the calibrated grid
     */
    fun saveGrid(grid: CalibratedGrid) {
        calibratedGrid = grid
    }

    /**
     * Get the currently saved grid
     */
    fun getGrid(): CalibratedGrid? {
        return calibratedGrid
    }

    /**
     * Check if a grid has been calibrated
     */
    fun hasGrid(): Boolean {
        return calibratedGrid != null
    }

    /**
     * Clear the saved grid
     */
    fun clearGrid() {
        calibratedGrid = null
    }
}
