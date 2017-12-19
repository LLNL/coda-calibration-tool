/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* This file is part of CCT. For details, see https://github.com/LLNL/coda-calibration-tool. 
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package llnl.gnem.core.gui.plotting.keymapper;

import java.awt.event.KeyEvent;

import llnl.gnem.core.gui.plotting.MouseMode;

/**
 * Created by: dodge1
 * Date: Dec 21, 2004
 */
public class DefaultControlKeyMapper implements ControlKeyMapper {
    public MouseMode getMouseMode( int keyCode )
    {
        if( keyCode == KeyEvent.VK_SHIFT ){
            return ( MouseMode.PAN );
        }
        else if( keyCode == KeyEvent.VK_ESCAPE ){
            return ( MouseMode.ZOOM_ONLY );
        }
        else if( keyCode == KeyEvent.VK_CONTROL ){
            return ( MouseMode.CONTROL_SELECT );
        }
        else if( keyCode == KeyEvent.VK_ALT ){
            return ( MouseMode.SELECT_REGION );
        }
        else
            return null;

    }

    public boolean isDeleteKey( int keyCode )
    {
        return keyCode == KeyEvent.VK_DELETE;
    }

    public boolean isControlKey( int keyCode )
    {
        return keyCode == KeyEvent.VK_CONTROL;
    }
}
