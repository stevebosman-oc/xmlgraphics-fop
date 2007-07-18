/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */
 
package org.apache.fop.fo.expr;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.fo.properties.ColorProperty;
import org.apache.fop.fo.properties.Property;

/**
 * Implements the rgb() function.
 */
class RGBColorFunction extends FunctionBase {
    
    /** {@inheritDoc} */
    public int nbArgs() {
        return 3;
    }

    /**
     * @return an object which implements the PercentBase interface.
     * Percents in arguments to this function are interpreted relative
     * to 255.
     */
    public PercentBase getPercentBase() {
        return new RGBPercentBase();
    }

    /** {@inheritDoc} */
    public Property eval(Property[] args,
                         PropertyInfo pInfo) throws PropertyException {
      FOUserAgent ua = (pInfo == null) 
              ? null 
              : (pInfo.getFO() == null ? null : pInfo.getFO().getUserAgent());
      return new ColorProperty(ua, "rgb(" + args[0] + "," + args[1] + "," + args[2] + ")");

    }

    static class RGBPercentBase implements PercentBase {
        public int getDimension() {
            return 0;
        }

        public double getBaseValue() {
            return 255f;
        }

        public int getBaseLength(PercentBaseContext context) {
            return 0;
        }

    }
}
