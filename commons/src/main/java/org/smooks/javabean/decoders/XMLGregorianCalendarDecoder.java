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
import org.smooks.javabean.DecodeType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * {@link javax.xml.datatype.XMLGregorianCalendar} data decoder.
 * <p/>
 * Decodes the supplied string into a {@link javax.xml.datatype.XMLGregorianCalendar} value based on the supplied "
 * {@link java.text.SimpleDateFormat format}" parameter, or the default (see below).
 * <p/>
 * The default date format used is "<i>yyyy-MM-dd'T'HH:mm:ss</i>" (see {@link java.text.SimpleDateFormat}). This format is based on the <a
 * href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#isoformats">ISO 8601</a> standard as used by the XML Schema type "<a
 * href="http://www.w3.org/TR/xmlschema-2/#dateTime">dateTime</a>".
 * <p/>
 * This decoder is synchronized on its underlying {@link java.text.SimpleDateFormat} instance.
 *
 * @author <a href="mailto:stefano.maestri@javalinux.it">stefano.maestri@javalinux.it</a>
 */
@DecodeType(XMLGregorianCalendar.class)
public class XMLGregorianCalendarDecoder extends DateDecoder {

    @Override
    public Object decode(String data) throws DataDecodeException {
        Date date = (Date) super.decode(data);

        try {
            GregorianCalendar gregCal = new GregorianCalendar();
            gregCal.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregCal);
        } catch (DatatypeConfigurationException e) {
            throw new DataDecodeException("Error decoding XMLGregorianCalendar data value '" + data + "' with decode format '" + format + "'.", e);
        }
    }

    @Override
    public String encode(Object date) throws DataDecodeException {
        if(!(date instanceof XMLGregorianCalendar)) {
            throw new DataDecodeException("Cannot encode Object type '" + date.getClass().getName() + "'.  Must be type '" + XMLGregorianCalendar.class.getName() + "'.");
        }
        return super.encode(((XMLGregorianCalendar)date).toGregorianCalendar().getTime());
    }
}