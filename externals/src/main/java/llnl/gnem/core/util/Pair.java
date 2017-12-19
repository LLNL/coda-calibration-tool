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
package llnl.gnem.core.util;

import java.io.Serializable;

/**
 * Created by: dodge1
 * Date: Feb 1, 2005
 */
public class Pair implements Serializable
{

	protected Object first = null;
	protected Object second = null;
	/**
	 *  Pair constructor comment.
	 */
	public Pair()
	{
	}
	/**
	 *  This method was created in VisualAge.
	 *
	 * @param  first java.lang.Object
	 * @param  second java.lang.Object
	 */
	public Pair(Object first, Object second)
	{
		this.first = first;
		this.second = second;
	}
	/**
	 *  This method was created in VisualAge.
	 *
	 * @param  newValue java.lang.Object
	 */
	public void setFirst(Object newValue)
	{
		this.first = newValue;
	}
	/**
	 *  This method was created in VisualAge.
	 *
	 * @param  newValue java.lang.Object
	 */
	public void setSecond(Object newValue)
	{
		this.second = newValue;
	}
	/**
	 *  This method was created in VisualAge.
	 *
	 * @return  java.lang.Object
	 */
	public Object getFirst()
	{
		return first;
	}
	/**
	 *  This method was created in VisualAge.
	 *
	 * @return  java.lang.Object
	 */
	public Object getSecond()
	{
		return second;
	}
	/**
	 *  This method was created in VisualAge.
	 *
	 * @return  java.lang.String
	 */
	public String toString()
	{
		return "{" + getFirst() + "," + getSecond() + "}";
	}
}
