/*
 *  Copyright 2011 Benjamin Glatzel <benjamin.glatzel@me.com>.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.github.begla.blockmania.blocks;

import com.github.begla.blockmania.utilities.Helper;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public abstract class Block {

    public static enum SIDE {

        LEFT, RIGHT, TOP, BOTTOM, FRONT, BACK;
    };
    private static Block[] _blocks = {new BlockAir(), new BlockGrass(), new BlockDirt(), new BlockStone(), new BlockWater(), new BlockWood(), new BlockLeaf(), new BlockSand(), new BlockHardStone(), new BlockRedFlower(), new BlockYellowFlower(), new BlockWheat(), new BlockWheatLarge()};
    private static BlockNil nilBlock = new BlockNil();


    /**
     * Returns the object for the given block type ID.
     * 
     * @param type Block type ID
     * @return The object for the given ID
     */
    public static Block getBlock(int type) {
        Block b = null;
        try {
            b = _blocks[type];
        } catch (Exception e) {
            b = nilBlock;
        }
        return b;
    }

    /**
     * Returns true if a given block type is translucent.
     *
     * @param type The block type
     * @return True if the block type is translucent
     */
    public boolean isBlockTypeTranslucent() {
        return false;
    }

    /**
     * Calculates the color offset for a given block type and a speific
     * side of the block.
     *
     * @param type The block type
     * @param side The block side
     * @return The color offset
     */
    public Vector4f getColorOffsetFor(SIDE side) {
        return new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Calculates the texture offset for a given block type and a specific
     * side of the block.
     *
     * @param type The type of the block
     * @param side The side of the block
     * @return The texture offset
     */
    public Vector2f getTextureOffsetFor(SIDE side) {
        return Helper.getInstance().calcOffsetForTextureAt(2, 0);
    }

    /**
     * Returns true, if the current block is a billboard.
     * 
     * @return True if billboard
     */
    public boolean isBlockBillboard() {
        return false;
    }

    /**
     * Returns true, if the block is invisible.
     *
     * @return True if invisible
     */
    public boolean isBlockInvisible() {
        return false;
    }

    /**
     * Returns true, if the block should be ignored
     * within the collision checks.
     *
     * @return True if penetrable
     */
    public boolean isPenetrable() {
        return false;
    }

    /**
     * Returns true, if the block should be considered
     * while calculating shadows.
     *
     * @return True if casting shadows
     */
    public boolean isCastingShadows() {
        return true;
    }
}