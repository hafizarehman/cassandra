/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db.marshal;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.serializers.TimestampSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.commons.lang3.time.DateUtils;

public class DateType extends AbstractType<Date>
{
    private static final Logger logger = LoggerFactory.getLogger(DateType.class);

    public static final DateType instance = new DateType();

    DateType() {} // singleton

    public int compare(ByteBuffer o1, ByteBuffer o2)
    {
        if (o1.remaining() == 0)
        {
            return o2.remaining() == 0 ? 0 : -1;
        }
        if (o2.remaining() == 0)
        {
            return 1;
        }

        return ByteBufferUtil.compareUnsigned(o1, o2);
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
      // Return an empty ByteBuffer for an empty string.
      if (source.isEmpty())
          return ByteBufferUtil.EMPTY_BYTE_BUFFER;

      return ByteBufferUtil.bytes(dateStringToTimestamp(source));
    }

    public static long dateStringToTimestamp(String source) throws MarshalException
    {
      long millis;

      if (source.toLowerCase().equals("now"))
      {
          millis = System.currentTimeMillis();
      }
      // Milliseconds since epoch?
      else if (source.matches("^\\d+$"))
      {
          try
          {
              millis = Long.parseLong(source);
          }
          catch (NumberFormatException e)
          {
              throw new MarshalException(String.format("unable to make long (for date) from: '%s'", source), e);
          }
      }
      // Last chance, attempt to parse as date-time string
      else
      {
          try
          {
              millis = DateUtils.parseDateStrictly(source, TimestampSerializer.iso8601Patterns).getTime();
          }
          catch (ParseException e1)
          {
              throw new MarshalException(String.format("unable to coerce '%s' to a  formatted date (long)", source), e1);
          }
      }

      return millis;
    }

    @Override
    public boolean isCompatibleWith(AbstractType<?> previous)
    {
        if (super.isCompatibleWith(previous))
            return true;

        if (previous instanceof TimestampType)
        {
            logger.warn("Changing from TimestampType to DateType is allowed, but be wary that they sort differently for pre-unix-epoch timestamps "
                      + "(negative timestamp values) and thus this change will corrupt your data if you have such negative timestamp. There is no "
                      + "reason to switch from DateType to TimestampType except if you were using DateType in the first place and switched to "
                      + "TimestampType by mistake.");
            return true;
        }

        return false;
    }

    @Override
    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.TIMESTAMP;
    }

    public TypeSerializer<Date> getSerializer()
    {
        return TimestampSerializer.instance;
    }
}
