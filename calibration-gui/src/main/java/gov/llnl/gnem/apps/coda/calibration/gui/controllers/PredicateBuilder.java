/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

/***
 * The predicate builder is used by the filter controller to manage predicates
 * used for filtering the table view results in the DataFilterController. With
 * this helper class, the filter can link together multiple And/Or predicates as
 * a single predicate. Active predicates are used while inactive ones are stored
 * until activated.
 *
 * @author downie4
 *
 * @param <T>
 */

class PredicateBuilder<T> {

    public interface ValueComparer<T> {
        boolean equatesTrue(T data, Object item);
    }

    private HashMap<String, Predicate<T>> predicates;
    private HashMap<String, Predicate<T>> activePredicates;

    PredicateBuilder() {
        this.predicates = new HashMap<>();
        this.activePredicates = new HashMap<>();
    }

    /***
     * Creates a predicate which can then be used by the predicate builder. Adds
     * it to the list of stored predicates but leaves it inactive.
     *
     * @param name
     *            The string name to give the new predicate.
     * @param comparer
     *            The comparer to use for comparing between items by the
     *            predicate.
     * @param item
     *            The value which will be compared with in the predicate.
     */
    public void setPredicate(String name, ValueComparer<T> comparer, Object item) {
        Predicate<T> predicate = createPredicate(comparer, item);
        if (predicate != null) {
            predicates.put(name, predicate);
        } else {
            predicates.remove(name);
        }
    }

    public void setPredicate(String name, Predicate<T> predicate) {
        if (predicate != null) {
            predicates.put(name, predicate);
        } else {
            predicates.remove(name);
        }
    }

    /***
     * Activates/deactivate a predicate. If the predicate is active, then it
     * will be used by the predicate builder for comparisons.
     *
     * @param name
     *            Name of the predicate to activate/deactivate.
     * @param active
     *            If true, the predicate will be activated otherwise it will be
     *            deactivated.
     */
    public void setPredicateActiveState(String name, Boolean active) {
        Predicate<T> predicate = predicates.get(name);
        if (predicate != null) {
            if (Boolean.TRUE.equals(active)) {
                activePredicates.put(name, predicate);
            } else {
                activePredicates.remove(name);
            }
        }
    }

    /***
     * Deactivates a group of predicates, useful for toggle groups.
     *
     * @param groupNames
     *            A list of the predicate names that should be deactivated.
     */
    public void deactivateGroup(List<String> groupNames) {
        groupNames.forEach(name -> {
            setPredicateActiveState(name, false);
        });
    }

    /***
     * Combines all the activate predicates into a single 'And' predicate. This
     * means that all activate predicates must evaluate to true for the returned
     * predicate to return true.
     *
     * @return A new 'And' predicate of type T.
     */
    public Predicate<T> getAndPredicate() {
        return activePredicates.values().stream().reduce(Predicate::and).orElse(x -> true);
    }

    /***
     * Combines all the activate predicates into a single 'Or' predicate. This
     * means that if any activate predicate evaluates to true returned predicate
     * will return true.
     *
     * @return A new 'And' predicate of type T.
     */
    public Predicate<T> getOrPredicate() {
        return activePredicates.values().stream().reduce(Predicate::or).orElse(x -> false);
    }

    /***
     * Creates a basic predicate.
     *
     * @param comparer
     *            The comparer to use for making comparisons in by the
     *            predicate.
     * @param item
     *            The string value to use by the predicate.
     * @return A new predicate that will evaluate true if comparer determines
     *         item and incoming value are equal
     */
    private Predicate<T> createPredicate(ValueComparer<T> comparer, Object item) {
        if (comparer == null || item == null) {
            return null;
        }
        return data -> comparer.equatesTrue(data, item);
    }
}