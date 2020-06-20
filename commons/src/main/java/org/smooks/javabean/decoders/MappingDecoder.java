/*-
 * ========================LICENSE_START=================================
 * Smooks Commons
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 * 
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 * 
 * ======================================================================
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
 * 
 * ======================================================================
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.javabean.decoders;

import org.smooks.javabean.DataDecodeException;
import org.smooks.javabean.DataDecoder;
import org.smooks.config.Configurable;

import java.util.Properties;

/**
 * Mapping decoder.
 * <p/>
 * Decodes the supplied data by using it to lookup the decode mapping from the
 * decoders configuration parameters.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class MappingDecoder implements DataDecoder, Configurable {
    
    private Properties resourceConfig;
    private boolean strict = true;

    public void setConfiguration(Properties resourceConfig) {
        this.resourceConfig = resourceConfig;
        strict = resourceConfig.getProperty("strict", "true").equals("true");
    }

    public Properties getConfiguration() {
        return resourceConfig;
    }

    public Object decode(String data) throws DataDecodeException {
        if(resourceConfig != null) {
            String mappingValue = resourceConfig.getProperty(data);

            if(mappingValue == null) {
                if(strict) {
                    throw new DataDecodeException("Mapping <param> for data '" + data + "' not defined.");
                } else {
                    return data;
                }
            }

            return mappingValue;
        } else {
            return data;
        }
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}