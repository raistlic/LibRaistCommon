/*
 * Copyright 2015 Lei CHEN (raistlic@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raistlic.common.codec;

/**
 * This class wraps a decoder to do the decode job, and leaves the encode job to its sub class
 * implementation.
 */
public abstract class DecoderCodecAdapter<S, D> implements Codec<S, D> {

  private Decoder<S, ? super D> decoder;

  public DecoderCodecAdapter(Decoder<S, ? super D> decoder) {

    if (decoder == null)
      throw new NullPointerException("decoder is null.");

    this.decoder = decoder;
  }

  @Override
  public final S decode(D target) {

    return decoder.decode(target);
  }
}
