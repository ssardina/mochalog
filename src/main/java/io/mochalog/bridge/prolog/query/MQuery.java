/*
 * Copyright 2017 The Mochalog Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mochalog.bridge.prolog.query;

import io.mochalog.bridge.prolog.lang.Module;

import io.mochalog.util.format.AbstractFormatter;
import org.jpl7.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents query provided to the SWI-Prolog
 * interpreter, managing localised query namespace.
 */
public class MQuery
{
    /**
     * Formatter of Prolog query strings using substitution rules
     * and domain-specific syntax
     */
    public static class Formatter extends AbstractFormatter
    {
        // Pattern corresponding to a Prolog compound structure
        // e.g. atom(arg1, arg2, ...)
        private final Pattern COMPOUND_PATTERN;
        // Syntactical representation of replacement of an
        // existing term for a new given term
        private final Pattern TERM_SETTER_PATTERN;

        /**
         * Constructor.
         */
        public Formatter()
        {
            super();

            // Interpret the data as ATOMIC: atoms, numbers (integers and floats), compound terms, variables, and lists.
            setRule("A", (i, o) -> String.valueOf(o));
            // Interpret the argument as a whole ATOM, whatever it is. Basically it is like adding quotes '....'
            //      For example if we to pass atom 'Variable' but not be treated as variable, or 'hello world'
            setRule("S", (i, o) -> "\"" + String.valueOf(o) + "\"");
            // Interpret as an integer
            setRule("I", Formatter::formatInteger);

            // Doc on regexp in Java:
            // https://docs.oracle.com/javase/tutorial/essential/regex/index.html
            // http://www.vogella.com/tutorials/JavaRegularExpressions/article.html
            // http://www.ntu.edu.sg/home/ehchua/programming/howto/regexe.html

            // Basic regex pattern defining a Prolog compound
            // reg expression naked: \w+\([^(]*\)
            // A non-empty word \w+
            //      followed by opening parentheis \(
            //      followed by zero or more non opening parenthesis [^(]*
            //      followed by closing parenthesis \)
            COMPOUND_PATTERN = Pattern.compile("\\w+\\([^(]*\\)");

            // Basic regex capturing most Prolog BASIC term instances
            // (integers, floats, strings, atoms)
            // TODO: Not a robust grammar definition of a Prolog term: it misses neasted terms badly, see MochaTest2
            //          can it actually be done with a regular expresion given that it is context-free?!?!?!
            //
            // regexp: \w+ | [0-9]+.[0-9] | "[^"]*"
            //  a non empty word \w+ (including integers) OR
            //  a number with decimals [0-9]+.[0-9]
            //  a string (any queryText quoted): "[^"]*"
            final String TERM_DEFINITION = "\\w+|[0-9]+.[0-9]|\"[^\"]*\"";


            // Rule of the form <PREVIOUS_VALUE> <- <NEW_VALUE>
            // Allows Prolog term data (PREVIOUS_VALUE) to be replaced with NEW_VALUE within
            // a query
            TERM_SETTER_PATTERN = Pattern.compile(
                String.format("(%1$s)\\s*<-\\s*(%1$s)", TERM_DEFINITION)
            );
        }

        @Override
        public String format(String str, Object... args) throws IllegalFormatException
        {
            String formattedStr = super.format(str, args);

            StringBuffer queryBuffer = new StringBuffer();
            Matcher compoundMatcher = COMPOUND_PATTERN.matcher(formattedStr);

            // Query compound terms placed at end of resultant query
            // to facilitate replacement of specified values
            StringBuilder setterCompounds = new StringBuilder();


//            System.out.println("==========================================");
//            System.out.println(str);
//            System.out.println();

            // Find instances of compounds in the query string
            while (compoundMatcher.find())
            {
                String compound = compoundMatcher.group();


//                System.out.println(compound);


                // Run a regex query over the query compound, looking
                // for instances of setter syntax
                Matcher setterMatcher = TERM_SETTER_PATTERN.matcher(compound);

                // Check for instances of setter syntax
                if (setterMatcher.find())
                {
                    // Existing version of the compound term will be solely
                    // constructed of 'previousValue' terms
                    String previousCompound = setterMatcher.replaceAll("$1");
                    // New version will be solely constructed of 'newValue' terms
                    String newCompound = setterMatcher.replaceAll("$2");

                    compoundMatcher.appendReplacement(queryBuffer,
                        Matcher.quoteReplacement(previousCompound));

                    // Add the necessary setter constructs to the end of the query
                    setterCompounds.append(", retractall(").append(previousCompound).append(")");
                    setterCompounds.append(", assertz(").append(newCompound).append(")");
                }
            }

            compoundMatcher.appendTail(queryBuffer);
            return queryBuffer.toString() + setterCompounds.toString();
        }

        /**
         * Format a given object argument into an integer string
         * with Prolog specifications
         * @param identifier Rule identifier
         * @param o Object substitution argument
         * @return Formatted integer replacement string
         * @throws IllegalFormatException Unable to convert given object
         * into an integer
         */
        private static String formatInteger(String identifier, Object o)
            throws IllegalFormatException
        {
            if (!(o instanceof Number))
            {
                // TODO: Currently only gives the first character of the identifier
                // This is reasonable currently due to all conversion codes
                // being a single character - Should be changed in future (create
                // custom exception)
                throw new IllegalFormatConversionException(identifier.charAt(0), o.getClass());
            }
            else
            {
                Number number = (Number) o;
                int intValue = number.intValue();
                return String.valueOf(intValue);
            }
        }
    }

    // String form of Prolog query
    private final String queryText;
    private final Term queryTerm;
    private final Query jplQuery;

    /**
     * Constructor based on a single string
     *
     * @param queryText Query string
     */
    public MQuery(String queryText)
    {
        this.queryText = queryText;
        this.queryTerm = Util.textToTerm(queryText);
        this.jplQuery = new Query(this.queryTerm);
    }


    /**
     * Constructor based on a single string
     *
     * @param queryTerm Query string
     */
    public MQuery(Term queryTerm)
    {
        this.queryText = null;
        this.queryTerm = queryTerm;
        this.jplQuery = new Query(this.queryTerm);
    }


    /**
     * Convert the query to string format
     * @return String format
     */
    @Override
    public String toString()
    {
        return queryText;
    }

    /**
     * Does this query have a textual representation?
     *
     * @return true if it has a textual representation
     */
    public boolean hasText() {
        return this.queryText != null;
    }

    /**
     * Formulate a query based on a format string and @-substitution arguments
     * @param query Formatted query string
     * @param args Query arguments
     * @return MQuery object
     */
    public static MQuery format(String query, Object... args)
    {
        Formatter formatter = new Formatter();
        String formattedQuery = formatter.format(query, args);
        return new MQuery(formattedQuery);
    }

    /**
     * Formulate a query based on a format string and @-substitution arguments
     * @param query Formatted query string
     * @param args Query arguments
     * @return MQuery object
     */
    public static MQuery jpl_format(String query, Term... args)
    {
        return new MQuery(Query1(query, args));
    }

    // Taken verbatim from JPL Query.java as it is a private there! :-(
    private static Term Query1(String text, Term[] args) {
        Term t = Util.textToTerm(text);
        if (t instanceof Atom) {
            return new Compound(text, args);
        } else {
            return t.putParams(args);
        }
    }



    /**
     * Generate a string form of the given query which
     * is runnable from the specified module
     * @param query Query to convert
     * @param module Module to run query from
     * @return Transformed query string
     */
    public static String runnableInModule(MQuery query, Module module)
    {
        return module == null ?
            query.toString() :
            String.format("%s:(%s)", module.getName(), query.toString());
    }

    @Override
    public final boolean equals(Object o)
    {
        // Early termination for self-identity
        if (this == o)
        {
            return true;
        }

        // null/type validation
        if (o != null && o instanceof MQuery)
        {
            MQuery query = (MQuery) o;
            // Field comparisons
            return Objects.equals(queryText, query.queryText);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(queryText);
    }
}
