/**
 * BSD 3-Clause License
 *
 * <p>
 * Copyright (c) 2026, Hannah Kullik
 *
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.model;

/**
 * Represents a single rule condition for a Smart Watchlist.
 */
public class SmartRule {
  private String field;
  private String operator;
  private String value;

  /**
   * Default constructor for Jackson JSON deserialization.
   */
  public SmartRule() {}

  /**
   * Constructs a new SmartRule with the specified condition parameters.
   *
   * @param field The movie property to evaluate (e.g., "Genre", "Year").
   * @param operator The comparison operator (e.g., "Equals", "Greater Than").
   * @param value The target value to match against.
   */
  public SmartRule(String field, String operator, String value) {
    this.field = field;
    this.operator = operator;
    this.value = value;
  }

  /**
   * Gets the movie property field this rule evaluates.
   *
   * @return The field name.
   */
  public String getField() {
    return field;
  }

  /**
   * Sets the movie property field for this rule.
   *
   * @param field The field name.
   */
  public void setField(String field) {
    this.field = field;
  }

  /**
   * Gets the comparison operator for this rule.
   *
   * @return The operator string.
   */
  public String getOperator() {
    return operator;
  }

  /**
   * Sets the comparison operator for this rule.
   *
   * @param operator The operator string.
   */
  public void setOperator(String operator) {
    this.operator = operator;
  }

  /**
   * Gets the target value this rule evaluates against.
   *
   * @return The target value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the target value for this rule.
   *
   * @param value The target value.
   */
  public void setValue(String value) {
    this.value = value;
  }
}
